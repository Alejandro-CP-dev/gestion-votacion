package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.config.AppContext;
import co.edu.sena.cimm.votaciones.dto.ApiError;
import co.edu.sena.cimm.votaciones.dto.GenerarTokenPropioRequest;
import co.edu.sena.cimm.votaciones.dto.GenerarTokenRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.service.TokenService;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rutas del padron:
 *
 *   POST /api/tokens/generar          [ADMIN] genera un token por estudiante activo
 *   POST /api/tokens/generar-propio   [ESTUDIANTE] el propio estudiante pide su token
 *   GET  /api/tokens/estado           [ADMIN] si un usuario tiene token y si ya voto
 *   GET  /api/tokens/mio              [ESTUDIANTE] el token (en claro) ya asignado al estudiante
 *
 * Las tres exigen X-Svis-Key: no estan en la lista de rutas publicas de
 * ApiKeyFilter. Eso no bloquea al estudiante: quien llama es siempre el
 * servidor PHP (que conoce la clave), a nombre del usuario de la sesion.
 */
@WebServlet("/api/tokens/*")
public class TokenServlet extends BaseApiServlet{
    
    private TokenService tokenService;
 
    @Override
    public void init() {
        this.tokenService = AppContext.getTokenService();
    }
 
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String ruta = rutaExtra(req);

            if (!"generar".equals(ruta) && !"generar-propio".equals(ruta)) {
                escribirJson(resp, 404, new ApiError("NO_ENCONTRADO", "La ruta no existe"));
                return;
            }

            // Esta respuesta lleva tokens en claro. No debe quedar guardada en
            // ninguna cache intermedia ni en el historial del navegador.
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");

            if ("generar-propio".equals(ruta)) {
                GenerarTokenPropioRequest peticion = leerJson(req, GenerarTokenPropioRequest.class);
                escribirJson(resp, 201, tokenService.generarTokenPropio(peticion));
                return;
            }

            GenerarTokenRequest peticion = leerJson(req, GenerarTokenRequest.class);
            escribirJson(resp, 201, tokenService.generarPadron(peticion));

        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }
 
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String ruta = rutaExtra(req);

            if (!"estado".equals(ruta) && !"mio".equals(ruta)) {
                escribirJson(resp, 404, new ApiError("NO_ENCONTRADO", "La ruta no existe"));
                return;
            }

            int encuestaId = aEntero(req.getParameter("encuestaId"), "encuestaId");
            int usuarioId = aEntero(req.getParameter("usuarioId"), "usuarioId");

            if ("mio".equals(ruta)) {
                // Igual que en el POST: esta respuesta lleva el token en claro.
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                escribirJson(resp, 200, tokenService.obtenerMiToken(encuestaId, usuarioId));
                return;
            }

            escribirJson(resp, 200, tokenService.consultarEstado(encuestaId, usuarioId));

        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }
 
    private int aEntero(String valor, String nombre) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new NegocioException("El parametro " + nombre + " debe ser un numero");
        }
    }
}
