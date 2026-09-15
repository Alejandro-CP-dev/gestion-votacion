package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.config.AppContext;
import co.edu.sena.cimm.votaciones.dto.ApiError;
import co.edu.sena.cimm.votaciones.dto.CrearEncuestaRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import co.edu.sena.cimm.votaciones.service.EncuestaService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rutas de encuestas:
 *
 *   POST /api/encuestas                   [ADMIN]   crea encuesta + opciones
 *   POST /api/encuestas/{id}/abrir        [ADMIN]   CREADA -> ACTIVA
 *   POST /api/encuestas/{id}/cerrar       [ADMIN]   ACTIVA -> CERRADA
 *   GET  /api/encuestas                   [ADMIN]   todas, o ?estado=CREADA
 *   GET  /api/encuestas/activas           [PUBLICO] las que admiten votos
 *   GET  /api/encuestas/{id}              [ADMIN]   una encuesta puntual
 *   GET  /api/encuestas/{id}/resultados   [ADMIN]   consolidado con conteos
 *
 * El mapeo "/api/encuestas/*" atrapa tambien "/api/encuestas" sin barra final,
 * asi que un solo servlet cubre las cuatro rutas.
 *
 * Sobre el orden de las comparaciones en doGet: "activas" se evalua ANTES de
 * intentar leer un numero. Si no, /api/encuestas/activas trataria de convertir
 * "activas" a entero y respondería un 400 confuso.
 */
@WebServlet("/api/encuestas/*")
public class EncuestaServlet extends BaseApiServlet {

    private EncuestaService encuestaService;

    @Override
    public void init() {
        this.encuestaService = AppContext.getEncuestaService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String ruta = rutaExtra(req);

            if (ruta == null) {
                CrearEncuestaRequest peticion = leerJson(req, CrearEncuestaRequest.class);
                escribirJson(resp, 201, encuestaService.crear(peticion));
                return;
            }

            if (ruta.endsWith("/abrir")) {
                int id = aEntero(ruta.substring(0, ruta.length() - "/abrir".length()));
                escribirJson(resp, 200, encuestaService.cambiarEstado(id, EstadoEncuesta.ACTIVA));
                return;
            }

            if (ruta.endsWith("/cerrar")) {
                int id = aEntero(ruta.substring(0, ruta.length() - "/cerrar".length()));
                escribirJson(resp, 200, encuestaService.cambiarEstado(id, EstadoEncuesta.CERRADA));
                return;
            }

            escribirJson(resp, 404, new ApiError(
                    "NO_ENCONTRADO", "La ruta solicitada no existe"));

        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String ruta = rutaExtra(req);
 
            if ("activas".equals(ruta)) {
                escribirJson(resp, 200, encuestaService.listarActivas());
                return;
            }
 
            if (ruta != null && ruta.endsWith("/resultados")) {
                int id = aEntero(ruta.substring(0, ruta.length() - "/resultados".length()));
                escribirJson(resp, 200, encuestaService.resultados(id));
                return;
            }
 
            if (ruta != null) {
                escribirJson(resp, 200, encuestaService.buscarPorId(aEntero(ruta)));
                return;
            }
 
            // Sin ruta extra: listado del administrador, con filtro opcional.
            escribirJson(resp, 200, encuestaService.listar(estadoDelParametro(req)));
 
        } catch (Exception ex) {
            responderError(resp, ex);
        }
    }

    
    /**
     * Lee ?estado=ACTIVA y lo convierte al enum. Ausente devuelve null, que
     * el servicio interpreta como "todas". Un valor invalido es un error del
     * cliente, no un listado vacio en silencio.
     */
    private EstadoEncuesta estadoDelParametro(HttpServletRequest req) {
        String valor = req.getParameter("estado");
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return EstadoEncuesta.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Estado invalido: " + valor
                    + ". Use CREADA, ACTIVA o CERRADA");
        }
    }
    
    /**
     * Convierte un tramo de la URL en numero. Si no lo es, es un error del
     * cliente (400), no una falla del servidor (500). Sin este metodo, el
     * NumberFormatException caeria en el catch general y responderia 500,
     * culpando al servidor de una URL mal escrita.
     */
    private int aEntero(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new NegocioException("El identificador debe ser un numero: " + valor);
        }
    }
}
