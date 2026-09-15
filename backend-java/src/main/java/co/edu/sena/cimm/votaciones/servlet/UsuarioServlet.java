package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.config.AppContext;
import co.edu.sena.cimm.votaciones.dto.ApiError;
import co.edu.sena.cimm.votaciones.dto.UsuarioView;
import co.edu.sena.cimm.votaciones.model.Usuario;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * GET /api/usuarios?rol=ESTUDIANTE   [ADMIN]   estudiantes activos, para el padron
 *
 * Solo soporta el listado que admin/padron.php necesita: los estudiantes
 * activos a los que se les puede generar token. No hay un listado general de
 * usuarios porque nada en el frontend lo pide todavia, y agregarlo ahora
 * seria una ruta que nadie prueba.
 */
@WebServlet("/api/usuarios")
public class UsuarioServlet extends BaseApiServlet {

    private UsuarioRepository usuarioRepository;

    @Override
    public void init() {
        this.usuarioRepository = AppContext.getUsuarioRepository();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String rol = req.getParameter("rol");

            if (rol == null || !"ESTUDIANTE".equalsIgnoreCase(rol)) {
                escribirJson(resp, 400, new ApiError("SOLICITUD_INVALIDA",
                        "El parametro rol es obligatorio y debe ser ESTUDIANTE"));
                return;
            }

            List<Usuario> estudiantes = usuarioRepository.listarEstudiantesActivos();
            List<UsuarioView> vista = estudiantes.stream()
                    .map(UsuarioView::desde)
                    .collect(Collectors.toList());

            escribirJson(resp, 200, vista);

        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }
}
