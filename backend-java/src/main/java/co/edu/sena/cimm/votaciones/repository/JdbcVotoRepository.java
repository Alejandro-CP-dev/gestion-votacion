/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.dto.ConflictoException;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import co.edu.sena.cimm.votaciones.model.EstadoToken;
import co.edu.sena.cimm.votaciones.util.TokenUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==================================================================== EL
 * CORAZON DEL TALLER
 * ====================================================================
 *
 * Emitir un voto son cinco operaciones que deben ocurrir todas o ninguna:
 *
 * 1. Bloquear la fila del token (SELECT ... FOR UPDATE) 2. Validar estado,
 * expiracion, encuesta y opcion 3. Quemar el token: DISPONIBLE -> USADO REGLA 2
 * 4. Sumar +1 al contador de la opcion REGLA 4 5. Registrar el comprobante
 * anonimo
 *
 * QUE PASA CON DOS PETICIONES SIMULTANEAS DEL MISMO TOKEN (REGLA 3):
 *
 * La peticion A toma el bloqueo de la fila en el paso 1. La peticion B llega al
 * mismo SELECT y NO falla: InnoDB la deja esperando. Cuando A hace commit y
 * suelta el bloqueo, B despierta, lee la fila actualizada, encuentra USADO y
 * aborta con 409.
 *
 * POR QUE EL "FOR UPDATE" ES IMPRESCINDIBLE Y NO SOLO CONVENIENTE:
 *
 * MySQL trabaja por defecto en REPEATABLE READ. Un SELECT normal ve una foto
 * del instante en que arranco la transaccion, asi que B veria DISPONIBLE aunque
 * A ya hubiera confirmado su voto, y las dos sumarian.
 *
 * Las lecturas con FOR UPDATE son la excepcion: leen siempre la ultima version
 * confirmada. Por eso ese SELECT tiene que ser la PRIMERA instruccion de la
 * transaccion.
 *
 * POR QUE NO SE BLOQUEA CON UN JOIN A Encuesta:
 *
 * "SELECT ... FROM TokenOtp t JOIN Encuesta e ... FOR UPDATE" bloquearia
 * tambien la fila de la encuesta, y como todos los votantes comparten esa fila,
 * TODOS los votos de la eleccion quedarian en fila india. El sistema seguiria
 * siendo correcto y seria inservible con 300 personas votando. Se bloquea la
 * fila del token y nada mas.
 */
public class JdbcVotoRepository implements VotoRepository {

    @Override
    public String emitir(String tokenHash, int opcionId) {

        Connection conn = null;
        try {

            conn = Database.getConnection();

            conn.setAutoCommit(false);

            // ---------- 1. Bloqueo de la fila del token ----------
            long tokenId;
            int encuestaId;
            EstadoToken estadoToken;
            LocalDateTime expiracion;

            String sqlBloqueo = "SELECT Id, EncuestaId, Estado, FechaExpiracion "
                    + "FROM TokenOtp WHERE TokenHash = ? FOR UPDATE";

            try (PreparedStatement ps = conn.prepareStatement(sqlBloqueo)) {

                ps.setString(1, tokenHash);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new NegocioException("El token no es valido");
                    }
                    tokenId = rs.getLong("Id");
                    encuestaId = rs.getInt("EncuestaId");
                    estadoToken = EstadoToken.valueOf(rs.getString("Estado"));
                    expiracion = rs.getTimestamp("FechaExpiracion").toLocalDateTime();
                }

            }

            // ---------- 2. Validaciones con la fila ya bloqueada ----------
            // Este es el punto donde la peticion que perdio la carrera aterriza.
            if (estadoToken == EstadoToken.USADO) {
                throw new ConflictoException("Este token ya fue utilizado. Solo se vota una vez");
            }

            if (expiracion.isBefore(LocalDateTime.now())) {
                throw new NegocioException("El token expiro y ya no permite votar");
            }

            if (estadoEncuesta(conn, encuestaId) != EstadoEncuesta.ACTIVA) {
                throw new NegocioException("La votacion no esta abierta en este momento");
            }

            // El opcionId llega por HTTP: nunca se usa sin verificar a que
            // encuesta pertenece. Sin esto, un token valido de la encuesta 1
            // podria sumarle un voto a una opcion de la encuesta 2.
            if (!opcionPertenece(conn, opcionId, encuestaId)) {
                throw new NegocioException("La opcion seleccionada no pertenece a esta votacion");
            }

            // ---------- 3. Quema del token (REGLA 2) ----------
            //
            // La guarda "AND Estado = 'DISPONIBLE'" es redundante teniendo el
            // bloqueo, y se deja a proposito: si alguien alguna vez quita el
            // FOR UPDATE, esta linea sigue impidiendo el voto doble. Una
            // defensa que cuesta cero se deja puesta.
            // TokenCifrado tambien se borra aqui: una vez quemado el token no
            // hay razon para seguir guardando una copia recuperable de su
            // texto plano, y borrarla reduce la ventana en la que una fuga de
            // la base de datos mas la llave de cifrado significarian algo.
            String sqlQuemar = "UPDATE TokenOtp SET Estado = 'USADO', UsadoEn = ?, TokenCifrado = NULL "
                    + "WHERE Id = ? AND Estado = 'DISPONIBLE'";

            try (PreparedStatement ps = conn.prepareStatement(sqlQuemar)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(2, tokenId);

                if (ps.executeUpdate() != 1) {
                    throw new ConflictoException("Este token ya fue utilizado. Solo se vota una vez");
                }
            }

            // ---------- 4. El voto (REGLA 4) ----------
            //
            // Toda la eleccion se reduce a esta linea. No se lee el contador,
            // no se suma en Java, no se vuelve a escribir: es MySQL quien
            // incrementa, en una sola operacion atomica. "ConteoVotos = ? " con
            // un valor calculado en Java seria otra condicion de carrera.
            String sqlSumar = "UPDATE Opcion SET ConteoVotos = ConteoVotos + 1 WHERE Id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sqlSumar)) {
                ps.setInt(1, opcionId);

                if (ps.executeUpdate() != 1) {
                    throw new NegocioException("No fue posible registrar el voto");
                }
            }

            // ---------- 5. Comprobante anonimo ----------
            String codigoRecibo = TokenUtil.sha256(UUID.randomUUID().toString());

            String sqlComprobante
                    = "INSERT INTO Comprobante (EncuestaId, CodigoRecibo) VALUES (?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sqlComprobante)) {
                ps.setInt(1, encuestaId);
                ps.setString(2, codigoRecibo);
                ps.executeUpdate();
            }

            // ---------- 6. Confirmacion ----------
            // Hasta esta linea, un corte de energia habria dejado la base de
            // datos exactamente como estaba.
            conn.commit();

            return codigoRecibo;

        } catch (NegocioException e) {
            // Cubre tambien ConflictoException, que hereda de esta.
            // Se revierte y se deja subir tal cual: el servlet necesita
            // distinguir 400 de 409, asi que envolverla en otra excepcion
            // convertiria la prueba de concurrencia en un 500.
            revertir(conn);
            throw e;

        } catch (SQLException e) {
            revertir(conn);
            throw new RuntimeException("Error al emitir el voto", e);

        } finally {
            cerrar(conn);
        }
    }

    // ==================== apoyo ====================
    /**
     * Lectura SIN bloqueo, y a proposito.
     *
     * La fila de la encuesta la comparten todos los votantes. Bloquearla
     * pondria la eleccion entera en fila india por algo que solo se esta
     * consultando.
     *
     * Usa la misma Connection que la transaccion: pedir otra al pool mientras
     * se sostiene un bloqueo es la receta para agotar el pool bajo carga.
     */
    private EstadoEncuesta estadoEncuesta(Connection cn, int encuestaId) throws SQLException {
        try (PreparedStatement ps
                = cn.prepareStatement("SELECT Estado FROM Encuesta WHERE Id = ?")) {
            ps.setInt(1, encuestaId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return EstadoEncuesta.valueOf(rs.getString("Estado"));
            }
        }
    }

    private boolean opcionPertenece(Connection cn, int opcionId, int encuestaId)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT 1 FROM Opcion WHERE Id = ? AND EncuestaId = ?")) {
            ps.setInt(1, opcionId);
            ps.setInt(2, encuestaId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void revertir(Connection cn) {
        if (cn == null) {
            return;
        }
        try {
            cn.rollback();
        } catch (SQLException e) {
            System.out.println("[SVIS] Fallo el rollback del voto: " + e.getMessage());
        }
    }

    private void cerrar(Connection cn) {
        if (cn == null) {
            return;
        }
        try {
            cn.setAutoCommit(true);
            cn.close();
        } catch (SQLException e) {
            System.out.println("[SVIS] Fallo al devolver la conexion al pool: " + e.getMessage());
        }
    }

}
