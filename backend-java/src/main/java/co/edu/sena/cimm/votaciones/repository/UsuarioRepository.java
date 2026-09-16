package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.model.EstadoUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla Usuario.
 *
 * Optional<Usuario> en vez de devolver null: quien llame al metodo esta
 * obligado por el compilador a considerar el caso "no existe". Con null, ese
 * caso se olvida hasta que aparece el NullPointerException en la sustentacion.
 */
public interface UsuarioRepository {

    /**
     * Usado por el login. El correo es UNIQUE, por eso devuelve uno solo.
     */
    Optional<Usuario> buscarPorCorreo(String correo);

    Optional<Usuario> buscarPorId(int id);

    /**
     * El padron: los estudiantes activos que recibiran token.
     *
     * Devuelve estudiantes y no todos los usuarios porque el administrador no
     * vota. Si votara, seria a la vez juez y parte del proceso.
     */
    List<Usuario> listarEstudiantesActivos();

    /**
     * Todos los usuarios, cualquier rol o estado. Para el panel del
     * administrador — el unico lugar que necesita ver la lista completa.
     */
    List<Usuario> listarTodos();

    /**
     * Inserta un usuario nuevo y devuelve el mismo objeto con el Id que
     * MySQL le asigno. Lanza ConflictoException si el correo ya existe.
     */
    Usuario crear(Usuario usuario);

    void actualizarPerfil(int id, String nombre, String apellido, String telefono);

    void actualizarClave(int id, String nuevoHash);

    void cambiarEstado(int id, EstadoUsuario estado);
}
