package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.config.AppContext;
import co.edu.sena.cimm.votaciones.dto.ActualizarPerfilRequest;
import co.edu.sena.cimm.votaciones.dto.ApiError;
import co.edu.sena.cimm.votaciones.dto.CambiarClaveRequest;
import co.edu.sena.cimm.votaciones.dto.CrearUsuarioRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.dto.UsuarioView;
import co.edu.sena.cimm.votaciones.model.EstadoUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import co.edu.sena.cimm.votaciones.service.UsuarioService;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rutas de usuarios:
 *
 *   GET  /api/usuarios?rol=ESTUDIANTE   [ADMIN]  estudiantes activos, para el padron
 *   GET  /api/usuarios                  [ADMIN]  listado completo
 *   POST /api/usuarios                  [ADMIN]  crear usuario
 *   POST /api/usuarios/{id}/activar     [ADMIN]  Estado -> ACTIVO
 *   POST /api/usuarios/{id}/desactivar  [ADMIN]  Estado -> INACTIVO
 *   PUT  /api/usuarios/{id}/perfil      [self]   editar nombre/apellido/telefono
 *   POST /api/usuarios/{id}/clave       [self]   cambiar clave (pide la actual)
 *
 * "[self]" no es un rol que el backend conozca: PHP es quien garantiza que el
 * id de estas dos rutas es siempre el de la sesion que hace la peticion,
 * nunca uno que venga de un formulario. Ver la nota de autorizacion en
 * UsuarioService.
 */
@WebServlet("/api/usuarios/*")
public class UsuarioServlet extends BaseApiServlet {

    private UsuarioRepository usuarioRepository;
    private UsuarioService usuarioService;

    @Override
    public void init() {
        this.usuarioRepository = AppContext.getUsuarioRepository();
        this.usuarioService = AppContext.getUsuarioService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String ruta = rutaExtra(req);

            if (ruta != null) {
                escribirJson(resp, 404, new ApiError(
                        "NO_ENCONTRADO", "La ruta solicitada no existe"));
                return;
            }

            String rol = req.getParameter("rol");

            if (rol != null) {
                if (!"ESTUDIANTE".equalsIgnoreCase(rol)) {
                    escribirJson(resp, 400, new ApiError("SOLICITUD_INVALIDA",
                            "El parametro rol solo admite ESTUDIANTE"));
                    return;
                }

                List<Usuario> estudiantes = usuarioRepository.listarEstudiantesActivos();
                List<UsuarioView> vista = estudiantes.stream()
                        .map(UsuarioView::desde)
                        .collect(Collectors.toList());

                escribirJson(resp, 200, vista);
                return;
            }

            escribirJson(resp, 200, usuarioService.listarTodos());

        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String ruta = rutaExtra(req);

            if (ruta == null) {
                CrearUsuarioRequest peticion = leerJson(req, CrearUsuarioRequest.class);
                escribirJson(resp, 201, usuarioService.crear(peticion));
                return;
            }

            if (ruta.endsWith("/activar")) {
                int id = aEntero(ruta.substring(0, ruta.length() - "/activar".length()));
                escribirJson(resp, 200, usuarioService.cambiarEstado(id, EstadoUsuario.ACTIVO));
                return;
            }

            if (ruta.endsWith("/desactivar")) {
                int id = aEntero(ruta.substring(0, ruta.length() - "/desactivar".length()));
                escribirJson(resp, 200, usuarioService.cambiarEstado(id, EstadoUsuario.INACTIVO));
                return;
            }

            if (ruta.endsWith("/clave")) {
                int id = aEntero(ruta.substring(0, ruta.length() - "/clave".length()));
                CambiarClaveRequest peticion = leerJson(req, CambiarClaveRequest.class);
                escribirJson(resp, 200, usuarioService.cambiarClave(id, peticion));
                return;
            }

            escribirJson(resp, 404, new ApiError(
                    "NO_ENCONTRADO", "La ruta solicitada no existe"));

        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String ruta = rutaExtra(req);

            if (ruta != null && ruta.endsWith("/perfil")) {
                int id = aEntero(ruta.substring(0, ruta.length() - "/perfil".length()));
                ActualizarPerfilRequest peticion = leerJson(req, ActualizarPerfilRequest.class);
                escribirJson(resp, 200, usuarioService.actualizarPerfil(id, peticion));
                return;
            }

            escribirJson(resp, 404, new ApiError(
                    "NO_ENCONTRADO", "La ruta solicitada no existe"));

        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }

    private int aEntero(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new NegocioException("El identificador debe ser un numero: " + valor);
        }
    }
}
