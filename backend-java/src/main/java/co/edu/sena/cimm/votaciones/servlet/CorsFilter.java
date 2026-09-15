
package co.edu.sena.cimm.votaciones.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Permite que el API sea consumido desde JavaScript en el navegador.
 *
 * Aclaracion para la sustentacion: el frontend PHP llama al backend con cURL
 * desde el servidor, y ahi CORS no aplica (es una regla del navegador, no del
 * protocolo). Este filtro sirve para probar con Postman o con fetch() desde la
 * consola, y para la pantalla de resultados si se refresca con JavaScript.
 *
 * El "*" es aceptable en un taller academico. En produccion se pondria el
 * dominio exacto del frontend.
 */
@WebFilter("/api/*")
public class CorsFilter implements Filter {

    @Override
    public void init(FilterConfig config) throws ServletException {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("Acces-Control-Allow-Origin", "*");
        resp.setHeader("Acces-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        resp.setHeader("Acces-Control-Allow-Headers", "Content-Type, Accept");

        //EL navegador manda un OPTIONS de sondeo antes del POST real.
        //Se responde 200 y se corta: no tiene que llegar a ningun servlet.
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

}
