package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.config.Configuracion;
import co.edu.sena.cimm.votaciones.dto.ApiError;
import co.edu.sena.cimm.votaciones.util.JsonUtil;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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
 * Exige la cabecera X-Svis-Key en los endpoints de administracion.
 *
 * EL PROBLEMA QUE RESUELVE: el $_SESSION['rol'] de PHP protege las pantallas
 * del frontend, no el API. Sin este filtro, cualquiera con la URL puede hacer
 * "curl -X POST /api/encuestas" y crear votaciones sin pasar por el login.
 *
 * LO QUE GARANTIZA Y LO QUE NO: prueba que quien llama es el servidor PHP, que
 * es el unico que conoce la clave. No prueba QUE USUARIO esta detras; de eso
 * responde la sesion del frontend, que ya verifico el rol antes de llamar. Es
 * una cadena de confianza de dos eslabones, y hay que poder decirlo asi.
 *
 * POR QUE /api/votos/emitir QUEDA PUBLICO: ahi la credencial es el token OTP,
 * que ya es unico, de un solo uso y verificado contra la base de datos. Pedir
 * ademas la clave compartida no agregaria seguridad y si impediria que el
 * instructor corra su script de doble voto directo contra el backend.
 */
@WebFilter("/api/*")
public class ApiKeyFilter implements Filter {

    /**
     * Rutas que no exigen la clave, evaluadas sobre la URI completa.
     */
    private static final List<String> RUTAS_PUBLICAS = Arrays.asList(
            "/api/salud",
            "/api/auth/login",
            "/api/encuestas/activas",
            "/api/votos/emitir"
    );

    private String claveEsperada;

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.claveEsperada = Configuracion.obtener("api.key", "SVIS_API_KEY", null);

        if (claveEsperada == null || claveEsperada.isEmpty()) {
            // Falla cerrado: sin clave configurada se bloquea la administracion.
            // Lo contrario (dejar pasar todo con una advertencia en el log)
            // convierte un error de despliegue en un agujero silencioso.
            System.out.println("[SVIS] ATENCION: api.key no esta configurada. "
                    + "Los endpoints de administracion quedaran bloqueados.");
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // El sondeo OPTIONS del navegador lo responde CorsFilter. Como el orden
        // entre filtros anotados no esta garantizado, este tambien lo deja pasar.
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String ruta = req.getRequestURI().substring(req.getContextPath().length());

        if (esPublica(ruta)) {
            chain.doFilter(request, response);
            return;
        }

        String claveRecibida = req.getHeader("X-Svis-Key");

        if (claveEsperada == null || claveEsperada.isEmpty()
                || !claveEsperada.equals(claveRecibida)) {

            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(JsonUtil.aJson(new ApiError(
                    "NO_AUTORIZADO",
                    "Esta operacion requiere una credencial de administracion valida")));
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean esPublica(String ruta) {
        for (String publica : RUTAS_PUBLICAS) {
            if (ruta.equals(publica)) {
                return true;
            }
        }
        return false;
    }
}
