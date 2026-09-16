package co.edu.sena.cimm.votaciones.service;

import co.edu.sena.cimm.votaciones.dto.ActualizarPerfilRequest;
import co.edu.sena.cimm.votaciones.dto.CambiarClaveRequest;
import co.edu.sena.cimm.votaciones.dto.CrearUsuarioRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.dto.NoEncontradoException;
import co.edu.sena.cimm.votaciones.dto.UsuarioView;
import co.edu.sena.cimm.votaciones.model.EstadoUsuario;
import co.edu.sena.cimm.votaciones.model.RolUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import co.edu.sena.cimm.votaciones.util.ClaveUtil;
import java.util.List;

/**
 * Reglas de negocio del padron de usuarios: alta, edicion de perfil, cambio
 * de clave y activar/desactivar.
 *
 * Sobre la autorizacion: esta clase no sabe quien esta haciendo la peticion,
 * solo el id que le llega. Que un usuario solo pueda tocar SU propio id es
 * algo que garantiza el frontend PHP (nunca acepta un id que no sea el de la
 * sesion actual), igual que ya hace admin/crear.php con "creadoPor". Aqui
 * solo se valida que las reglas de negocio se cumplan para el id recibido.
 */
public class UsuarioService {

    private static final int LARGO_MINIMO_CLAVE = 6;

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioView crear(CrearUsuarioRequest peticion) {

        if (peticion == null
                || vacio(peticion.nombre) || vacio(peticion.apellido)
                || vacio(peticion.correo) || vacio(peticion.clave)) {
            throw new NegocioException("Nombre, apellido, correo y clave son obligatorios");
        }

        if (peticion.clave.length() < LARGO_MINIMO_CLAVE) {
            throw new NegocioException(
                    "La clave debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(peticion.nombre.trim());
        usuario.setApellido(peticion.apellido.trim());
        usuario.setCorreo(peticion.correo.trim().toLowerCase());
        usuario.setTelefono(vacio(peticion.telefono) ? null : peticion.telefono.trim());
        usuario.setClave(ClaveUtil.hash(peticion.clave));
        usuario.setRol(rolDeTexto(peticion.rol));
        usuario.setEstado(EstadoUsuario.ACTIVO);

        return UsuarioView.desde(usuarioRepository.crear(usuario));
    }

    public List<UsuarioView> listarTodos() {
        return UsuarioView.desde(usuarioRepository.listarTodos());
    }

    public UsuarioView cambiarEstado(int id, EstadoUsuario nuevoEstado) {

        if (nuevoEstado == null) {
            throw new NegocioException("El estado es obligatorio y debe ser ACTIVO o INACTIVO");
        }

        Usuario usuario = usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));

        usuarioRepository.cambiarEstado(id, nuevoEstado);
        usuario.setEstado(nuevoEstado);
        return UsuarioView.desde(usuario);
    }

    public UsuarioView actualizarPerfil(int id, ActualizarPerfilRequest peticion) {

        if (peticion == null || vacio(peticion.nombre) || vacio(peticion.apellido)) {
            throw new NegocioException("Nombre y apellido son obligatorios");
        }

        Usuario usuario = usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));

        String nombre = peticion.nombre.trim();
        String apellido = peticion.apellido.trim();
        String telefono = vacio(peticion.telefono) ? null : peticion.telefono.trim();

        usuarioRepository.actualizarPerfil(id, nombre, apellido, telefono);

        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setTelefono(telefono);
        return UsuarioView.desde(usuario);
    }

    public UsuarioView cambiarClave(int id, CambiarClaveRequest peticion) {

        if (peticion == null || vacio(peticion.claveActual) || vacio(peticion.claveNueva)) {
            throw new NegocioException("La clave actual y la clave nueva son obligatorias");
        }

        if (peticion.claveNueva.length() < LARGO_MINIMO_CLAVE) {
            throw new NegocioException(
                    "La clave nueva debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
        }

        Usuario usuario = usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + id));

        if (!ClaveUtil.coincide(peticion.claveActual, usuario.getClave())) {
            throw new NegocioException("La contraseña actual no es correcta");
        }

        usuarioRepository.actualizarClave(id, ClaveUtil.hash(peticion.claveNueva));
        return UsuarioView.desde(usuario);
    }

    private RolUsuario rolDeTexto(String valor) {
        if (vacio(valor)) {
            return RolUsuario.ESTUDIANTE;
        }
        try {
            return RolUsuario.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Rol invalido: " + valor + ". Use ADMIN o ESTUDIANTE");
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
