package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.config.AppContext;
import co.edu.sena.cimm.votaciones.dto.ApiError;
import co.edu.sena.cimm.votaciones.dto.EmitirVotoRequest;
import co.edu.sena.cimm.votaciones.service.VotoService;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * POST /api/votos/emitir
 *
 * Entrada: { "token": "ABCDE-FGHJK", "opcionId": 7 } Salida: 200 {
 * "codigoRecibo": "a3f...", "mensaje": "..." } 409 { "codigo": "CONFLICTO",
 * "mensaje": "Este token ya fue..." }
 *
 * ENDPOINT PUBLICO, sin X-Svis-Key y sin sesion. No es un descuido:
 *
 * - La credencial es el token OTP, que ya es unico, de un solo uso y verificado
 * contra la base de datos dentro de la transaccion. - Exigir sesion obligaria a
 * que el backend supiera quien esta votando, y eso es justo lo que el diseno
 * evita. - El instructor tiene que poder disparar su script de doble voto
 * directo contra este endpoint, sin cabeceras adicionales.
 *
 * El 200 y el 409 son los codigos exactos que pide la evidencia 1 del taller.
 */
@WebServlet("/api/votos/*")
public class VotoServlet extends BaseApiServlet {

    private VotoService votoService;

    @Override
    public void init() {
        this.votoService = AppContext.getVotoService();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (!"emitir".equals(rutaExtra(req))) {
                escribirJson(resp, 404, new ApiError("NO_ENCONTRADO", "La ruta no existe"));
                return;
            }

            // El comprobante no debe quedar en cache de ningun intermediario.
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

            EmitirVotoRequest peticion = leerJson(req, EmitirVotoRequest.class);
            escribirJson(resp, 200, votoService.emitir(peticion));

        } catch (Exception ex) {
            // Aqui es donde ConflictoException se convierte en 409.
            responderError(resp, ex);
        }
    }
}
