package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.model.Encuesta;
import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import co.edu.sena.cimm.votaciones.model.Opcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementacion JDBC de Encuesta + Opcion.
 *
 * Aqui aparece la primera transaccion real del proyecto. Es el mismo esqueleto
 * que usara la emision del voto en el paso 6, pero sin bloqueo de filas:
 *
 *     setAutoCommit(false) -> operaciones -> commit()
 *     si algo falla        -> rollback()
 *     pase lo que pase     -> setAutoCommit(true) antes de soltar la conexion
 */
public class JdbcEncuestaRepository implements EncuestaRepository{
    
    @Override
    public Encuesta crear(Encuesta encuesta) {
 
        String sqlEncuesta =
                "INSERT INTO Encuesta (Titulo, Descripcion, Estado, CreadoPor) VALUES (?, ?, ?, ?)";
        String sqlOpcion =
                "INSERT INTO Opcion (EncuestaId, TextoOpcion, Orden) VALUES (?, ?, ?)";
 
        Connection cn = null;
        try {
            cn = Database.getConnection();
 
            // Desde esta linea, nada se escribe de verdad hasta el commit.
            cn.setAutoCommit(false);
 
            int encuestaId;
 
            // RETURN_GENERATED_KEYS: MySQL asigna el Id con AUTO_INCREMENT y
            // hay que recuperarlo para insertar las opciones. La alternativa
            // (SELECT MAX(Id)) es incorrecta con peticiones concurrentes:
            // devolveria el id de la encuesta que creo otro administrador.
            try (PreparedStatement ps =
                         cn.prepareStatement(sqlEncuesta, Statement.RETURN_GENERATED_KEYS)) {
 
                ps.setString(1, encuesta.getTitulo());
                ps.setString(2, encuesta.getDescripcion());
                ps.setString(3, EstadoEncuesta.CREADA.name());
                ps.setInt(4, encuesta.getCreadoPor());
                ps.executeUpdate();
 
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException("MySQL no devolvio el Id de la encuesta");
                    }
                    encuestaId = claves.getInt(1);
                }
            }
 
            // Las opciones van en lote: un solo viaje a la base de datos en
            // vez de uno por opcion.
            try (PreparedStatement ps = cn.prepareStatement(sqlOpcion)) {
                int orden = 1;
                for (Opcion o : encuesta.getOpcion()) {
                    ps.setInt(1, encuestaId);
                    ps.setString(2, o.getTextoOpcion());
                    ps.setInt(3, orden);
                    ps.addBatch();
                    orden++;
                }
                ps.executeBatch();
            }
 
            cn.commit();
 
            encuesta.setId(encuestaId);
            encuesta.setEstado(EstadoEncuesta.CREADA);
            
            // Las opciones se releen de la base de datos en vez de devolver
            // los objetos que llegaron.
            //
            // Los que llegaron no tienen Id (lo acaba de asignar el
            // AUTO_INCREMENT) ni Orden, asi que la respuesta saldria con
            // "id": 0 en cada opcion. El frontend renderizaria la papeleta con
            // value="0" y todos los votos serian rechazados.
            //
            // Es una consulta extra en una operacion que ocurre una vez por
            // eleccion. Devolver el estado real de la base de datos despues de
            // escribir vale mucho mas que ahorrarse ese viaje.
            encuesta.setOpcion(buscarOpciones(cn, encuestaId));
            return encuesta;
 
        } catch (SQLException e) {
            revertir(cn);
            throw new RuntimeException("Error al crear la encuesta", e);
 
        } finally {
            cerrar(cn);
        }
    }
 
    @Override
    public Optional<Encuesta> buscarPorId(int id) {
        String sql = "SELECT Id, Titulo, Descripcion, Estado, CreadoPor, CreadoEn "
                   + "FROM Encuesta WHERE Id = ?";
 
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
 
            ps.setInt(1, id);
 
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Encuesta e = mapearEncuesta(rs);
                e.setOpcion(buscarOpciones(cn, id));
                return Optional.of(e);
            }
 
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar la encuesta", e);
        }
    }
    
    @Override
    public List<Encuesta> listarTodas() {
        String sql = "SELECT Id, Titulo, Descripcion, Estado, CreadoPor, CreadoEn "
                   + "FROM Encuesta ORDER BY CreadoEn DESC";
 
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
 
            List<Encuesta> encuestas = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    encuestas.add(mapearEncuesta(rs));
                }
            }
 
            cargarOpcionesDeTodas(cn, encuestas);
            return encuestas;
 
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar las encuestas", e);
        }
    }
 
    @Override
    public List<Encuesta> listarPorEstado(EstadoEncuesta estado) {
        String sql = "SELECT Id, Titulo, Descripcion, Estado, CreadoPor, CreadoEn "
                   + "FROM Encuesta WHERE Estado = ? ORDER BY CreadoEn DESC";
 
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
 
            ps.setString(1, estado.name());
 
            List<Encuesta> encuestas = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    encuestas.add(mapearEncuesta(rs));
                }
            }
 
            cargarOpcionesDeTodas(cn, encuestas);
            return encuestas;
 
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar las encuestas", e);
        }
    }
 
    @Override
    public boolean cambiarEstado(int encuestaId, EstadoEncuesta estadoActual,
                                 EstadoEncuesta nuevoEstado) {
 
        // La guarda "AND Estado = ?" es lo que hace segura esta operacion.
        // Si dos administradores cierran la misma encuesta al tiempo, el
        // segundo UPDATE afecta 0 filas y el metodo devuelve false, en vez de
        // sobreescribir sin darse cuenta. Es la misma idea que protegera la
        // quema del token en el paso 6, en su version mas simple.
        String sql = "UPDATE Encuesta SET Estado = ? WHERE Id = ? AND Estado = ?";
 
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
 
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, encuestaId);
            ps.setString(3, estadoActual.name());
 
            return ps.executeUpdate() == 1;
 
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el estado de la encuesta", e);
        }
    }
 
    @Override
    public boolean opcionPerteneceAEncuesta(int opcionId, int encuestaId) {
        String sql = "SELECT 1 FROM Opcion WHERE Id = ? AND EncuestaId = ?";
 
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
 
            ps.setInt(1, opcionId);
            ps.setInt(2, encuestaId);
 
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
 
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar la opcion", e);
        }
    }
 
    // ==================== apoyo ====================
 
    private List<Opcion> buscarOpciones(Connection cn, int encuestaId) throws SQLException {
        String sql = "SELECT Id, EncuestaId, TextoOpcion, Orden, ConteoVotos "
                   + "FROM Opcion WHERE EncuestaId = ? ORDER BY Orden";
 
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
 
            List<Opcion> opciones = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    opciones.add(mapearOpcion(rs));
                }
            }
            return opciones;
        }
    }
 
    /**
     * Trae las opciones de TODAS las encuestas en una sola consulta.
     *
     * Lo facil seria recorrer la lista llamando buscarOpciones() por cada una.
     * Con 20 encuestas eso son 21 viajes a la base de datos: es el problema
     * N+1, la causa mas comun de que una pantalla se vuelva lenta sin que
     * ninguna consulta se vea mal por separado.
     *
     * Nota sobre el IN dinamico: lo que se concatena son los signos "?", nunca
     * los valores. Los valores siguen entrando por setInt(). Concatenar
     * marcadores es seguro; concatenar datos es una inyeccion SQL.
     */
    private void cargarOpcionesDeTodas(Connection cn, List<Encuesta> encuestas) throws SQLException {
        if (encuestas.isEmpty()) {
            return;
        }
 
        StringBuilder marcadores = new StringBuilder();
        for (int i = 0; i < encuestas.size(); i++) {
            marcadores.append(i == 0 ? "?" : ",?");
        }
 
        String sql = "SELECT Id, EncuestaId, TextoOpcion, Orden, ConteoVotos FROM Opcion "
                   + "WHERE EncuestaId IN (" + marcadores + ") ORDER BY EncuestaId, Orden";
 
        Map<Integer, List<Opcion>> porEncuesta = new HashMap<>();
 
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < encuestas.size(); i++) {
                ps.setInt(i + 1, encuestas.get(i).getId());
            }
 
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Opcion o = mapearOpcion(rs);
                    porEncuesta.computeIfAbsent(o.getEncuestaId(), k -> new ArrayList<>()).add(o);
                }
            }
        }
 
        for (Encuesta e : encuestas) {
            e.setOpcion(porEncuesta.get(e.getId()));
        }
    }
 
    private Encuesta mapearEncuesta(ResultSet rs) throws SQLException {
        Encuesta e = new Encuesta();
        e.setId(rs.getInt("Id"));
        e.setTitulo(rs.getString("Titulo"));
        e.setDescripcion(rs.getString("Descripcion"));
        e.setEstado(EstadoEncuesta.valueOf(rs.getString("Estado")));
        e.setCreadoPor(rs.getInt("CreadoPor"));
        if (rs.getTimestamp("CreadoEn") != null) {
            e.setCreadoEn(rs.getTimestamp("CreadoEn").toLocalDateTime());
        }
        return e;
    }
 
    private Opcion mapearOpcion(ResultSet rs) throws SQLException {
        Opcion o = new Opcion();
        o.setId(rs.getInt("Id"));
        o.setEncuestaId(rs.getInt("EncuestaId"));
        o.setTextoOpcion(rs.getString("TextoOpcion"));
        o.setOrden(rs.getInt("Orden"));
        o.setConteoVotos(rs.getLong("ConteoVotos"));
        return o;
    }
 
    private void revertir(Connection cn) {
        if (cn == null) {
            return;
        }
        try {
            cn.rollback();
        } catch (SQLException e) {
            System.out.println("[SVIS] Fallo el rollback: " + e.getMessage());
        }
    }
 
    /**
     * Devuelve la conexion al pool dejandola como la recibio.
     *
     * El setAutoCommit(true) antes del close importa: la conexion no se cierra,
     * vuelve al pool y se la entrega a la siguiente peticion. Si queda en
     * modo transaccional, esa peticion escribiria sin hacer commit nunca y sus
     * cambios se perderian en silencio.
     */
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
