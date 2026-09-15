package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.model.EstadoUsuario;
import co.edu.sena.cimm.votaciones.model.RolUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC del padron de usuarios.
 *
 * TRES REGLAS QUE SE REPITEN EN TODOS LOS REPOSITORIOS DEL PROYECTO:
 *
 * 1. PreparedStatement con "?" SIEMPRE. Nunca concatenar valores en el SQL.
 * "WHERE Correo = '" + correo + "'" permite que alguien escriba ' OR '1'='1 en
 * el formulario de login y entre sin clave.
 *
 * 2. try-with-resources en Connection, PreparedStatement y ResultSet. El
 * close() de la conexion la devuelve al pool; si se olvida, el pool se agota y
 * la aplicacion se cuelga esperando una conexion que nadie libero.
 *
 * 3. Columnas listadas una por una, nunca SELECT *. Asi el mapeo no depende del
 * orden de las columnas en la tabla y agregar una columna manana no rompe nada.
 */
public class JdbcUsuarioRepository implements UsuarioRepository {

    private static final String columnas = "Id, Nombre, Apellido, Correo, Telefono, Clave, Rol, Estado";

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {

        String consulta = "SELECT " + columnas + " FROM Usuario WHERE Correo = ? ";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(consulta)) {

            ps.setString(1, correo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el usuario por correo", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) {

        String consulta = "SELECT " + columnas + " FROM Usuario WHERE Id = ? ";

        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(consulta)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el usuario por su ID ", e);
        }
    }

    @Override
    public List<Usuario> listarEstudiantesActivos() {

        String consulta = "SELECT " + columnas + " FROM Usuario "
                + "WHERE Rol = 'ESTUDIANTE' AND Estado = 'ACTIVO' "
                + "ORDER BY Apellido, Nombre";

        List<Usuario> lista = new ArrayList<>();

        try (Connection cn = Database.getConnection(); PreparedStatement ps = cn.prepareStatement(consulta); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los estudiantes activos", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("Id"));
        u.setNombre(rs.getString("Nombre"));
        u.setApellido(rs.getString("Apellido"));
        u.setCorreo(rs.getString("Correo"));
        u.setTelefono(rs.getString("Telefono"));
        u.setClave(rs.getString("Clave"));
        u.setRol(RolUsuario.valueOf(rs.getString("Rol")));
        u.setEstado(EstadoUsuario.valueOf(rs.getString("Estado")));
        return u;
    }
}
