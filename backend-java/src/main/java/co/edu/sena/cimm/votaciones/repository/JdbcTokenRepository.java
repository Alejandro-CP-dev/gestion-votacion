/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.dto.ConflictoException;
import co.edu.sena.cimm.votaciones.model.TokenOtp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.List;

/**
 *
 * @author Usuario
 */
public class JdbcTokenRepository implements TokenRepository {

    @Override
    public int guardarPadron(List<TokenOtp> tokens) {

        String sql = "INSERT INTO TokenOtp "
                + "(EncuestaId, UsuarioId, TokenHash, Estado, FechaExpiracion) "
                + "VALUES (?, ?, ?, ?, ?)";

        Connection cn = null;
        try {
            cn = Database.getConnection();
            cn.setAutoCommit(false);

            int guardados;

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                for (TokenOtp t : tokens) {
                    ps.setInt(1, t.getEncuestaId());
                    ps.setInt(2, t.getUsuarioId());
                    ps.setString(3, t.getTokenHash());
                    ps.setString(4, t.getEstado().name());
                    ps.setTimestamp(5, Timestamp.valueOf(t.getFechaExpiracion()));
                    ps.addBatch();
                }

                int[] resultados = ps.executeBatch();
                guardados = resultados.length;
            }

            cn.commit();
            return guardados;

        } catch (SQLIntegrityConstraintViolationException e) {
            // Choco con UNIQUE(EncuestaId, UsuarioId) o con UNIQUE(TokenHash).
            //
            // ESTA EXCEPCION ES LA REGLA 1 FUNCIONANDO. La verificacion previa
            // del servicio (existePadron) es solo para dar un mensaje amable;
            // entre esa consulta y este INSERT cabe otra peticion haciendo lo
            // mismo. La garantia real de que nadie tenga dos tokens no esta en
            // ningun "if" de Java: esta en la restriccion de la base de datos,
            // que es el unico punto por el que pasan todas las peticiones.
            revertir(cn);
            throw new ConflictoException("Ya existe un padron de tokens para esta encuesta");

        } catch (SQLException e) {
            revertir(cn);
            throw new RuntimeException("Error al generar el padron de tokens", e);

        } finally {
            cerrar(cn);
        }
    }

    @Override
    public boolean existePadron(int encuestaId) {
        return existe("SELECT 1 FROM TokenOtp WHERE EncuestaId = ? LIMIT 1", encuestaId, null);
    }

    @Override
    public boolean tieneToken(int encuestaId, int usuarioId) {
        return existe("SELECT 1 FROM TokenOtp WHERE EncuestaId = ? AND UsuarioId = ?",
                encuestaId, usuarioId);
    }

    @Override
    public int contarHabilitados(int encuestaId) {
        return contar("SELECT COUNT(*) FROM TokenOtp WHERE EncuestaId = ?", encuestaId);
    }

    @Override
    public int contarVotantes(int encuestaId) {
        return contar("SELECT COUNT(*) FROM TokenOtp WHERE EncuestaId = ? AND Estado = 'USADO'",
                encuestaId);
    }

    @Override
    public boolean usuarioYaVoto(int encuestaId, int usuarioId) {
        return existe("SELECT 1 FROM TokenOtp "
                + "WHERE EncuestaId = ? AND UsuarioId = ? AND Estado = 'USADO'",
                encuestaId, usuarioId);
    }

    // ==================== apoyo ====================
    /**
     * Consulta de existencia con uno o dos parametros.
     */
    
    private int contar(String sql, int encuestaId) {
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
 
            ps.setInt(1, encuestaId);
 
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
 
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar tokens de la encuesta", e);
        }
    }
    
    private boolean existe(String sql, int primero, Integer segundo) {
        try (Connection cn = Database.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, primero);
            if (segundo != null) {
                ps.setInt(2, segundo);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el padron de tokens", e);
        }
    }

    private void revertir(Connection cn) {
        if (cn == null) {
            return;
        }
        try {
            cn.rollback();
        } catch (SQLException e) {
            System.out.println("[SVIS] Fallo el rollback del padron: " + e.getMessage());
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
