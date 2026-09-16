package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.dto.ConflictoException;
import co.edu.sena.cimm.votaciones.model.EstadoUsuario;
import co.edu.sena.cimm.votaciones.model.RolUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
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

    @Override
    public List<Usuario> listarTodos() {

        String consulta = "SELECT " + columnas + " FROM Usuario ORDER BY Apellido, Nombre";

        List<Usuario> lista = new ArrayList<>();

        try (Connection cn = Database.getConnection(); PreparedStatement ps = cn.prepareStatement(consulta); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;

        } catch (SQLException e) {
            throw new RuntimeException("Error al listar los usuarios", e);
        }
    }

    @Override
    public Usuario crear(Usuario usuario) {

        String sql = "INSERT INTO Usuario (Nombre, Apellido, Correo, Telefono, Clave, Rol, Estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection cn = Database.getConnection();
                PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellido());
            ps.setString(3, usuario.getCorreo());
            ps.setString(4, usuario.getTelefono());
            ps.setString(5, usuario.getClave());
            ps.setString(6, usuario.getRol().name());
            ps.setString(7, usuario.getEstado().name());
            ps.executeUpdate();

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("MySQL no devolvio el Id del usuario");
                }
                usuario.setId(claves.getInt(1));
            }
            return usuario;

        } catch (SQLIntegrityConstraintViolationException e) {
            // Choco con UqUsuarioCorreo. La verificacion previa en el servicio
            // es solo para dar un mensaje amable; la garantia real de que no
            // haya dos usuarios con el mismo correo es esta restriccion.
            throw new ConflictoException("Ya existe un usuario con ese correo");

        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el usuario", e);
        }
    }

    @Override
    public void actualizarPerfil(int id, String nombre, String apellido, String telefono) {

        String sql = "UPDATE Usuario SET Nombre = ?, Apellido = ?, Telefono = ? WHERE Id = ?";

        try (Connection cn = Database.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            ps.setString(2, apellido);
            ps.setString(3, telefono);
            ps.setInt(4, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el perfil del usuario", e);
        }
    }

    @Override
    public void actualizarClave(int id, String nuevoHash) {

        String sql = "UPDATE Usuario SET Clave = ? WHERE Id = ?";

        try (Connection cn = Database.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nuevoHash);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la clave del usuario", e);
        }
    }

    @Override
    public void cambiarEstado(int id, EstadoUsuario estado) {

        String sql = "UPDATE Usuario SET Estado = ? WHERE Id = ?";

        try (Connection cn = Database.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, estado.name());
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el estado del usuario", e);
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
