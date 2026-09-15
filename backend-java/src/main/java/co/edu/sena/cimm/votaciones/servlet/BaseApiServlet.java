package co.edu.sena.cimm.votaciones.servlet;

import co.edu.sena.cimm.votaciones.dto.ApiError;
import co.edu.sena.cimm.votaciones.dto.ConflictoException;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.dto.NoEncontradoException;
import co.edu.sena.cimm.votaciones.util.JsonUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Clase padre de todos los servlets del API. Concentra tres cosas que de otro
 * modo se repetirian en cada uno: escribir JSON, leer JSON y traducir
 * excepciones a codigos HTTP.
 *
 * La traduccion vive AQUI y no en cada servlet a proposito. Si manana el
 * taller pide que los conflictos respondan 423 en vez de 409, se cambia en un
 * solo lugar. En el proyecto de referencia ese try/catch esta copiado en cada
 * metodo, y basta que uno quede distinto para que el API sea inconsistente.
 */
public abstract class BaseApiServlet extends HttpServlet {
    
    protected void escribirJson(HttpServletResponse resp, int estado, Object cuerpo) throws IOException{
        resp.setStatus(estado);
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(JsonUtil.aJson(cuerpo));        
    }
    
    /**
     * Lee el cuerpo de la peticion y lo convierte al DTO indicado.
     *
     * El setCharacterEncoding("UTF-8") es obligatorio y facil de olvidar: sin
     * el, Tomcat asume ISO-8859-1 y los titulos con tildes o enes llegan rotos
     * a la base de datos.
     */
    protected <T> T leerJson(HttpServletRequest req, Class<T> clase) throws IOException{
        req.setCharacterEncoding("UTF-8");
        String cuerpo;
        try (BufferedReader lector = req.getReader()){
            cuerpo = lector.lines().collect(Collectors.joining("\n"));
        }
        if (cuerpo == null || cuerpo.trim().isEmpty()) {
            throw new NegocioException("El cuerpo de la peticion viene vacio");
        }
        return JsonUtil.desdeJson(cuerpo, clase);
    }
    
    /**
     * Extrae el segmento de la URL que sigue a la ruta del servlet.
     * Para GET /api/encuestas/7/resultados devuelve "7/resultados".
     * Devuelve null si no hay nada despues.
     */
    protected String rutaExtra(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.length() <= 1) {
            return null;
        }
        return info.substring(1);
    }
    
    /**
    *Unico punto de traduccion de excepcion a codigo HTTP.
    * 
    * El Orden importa: NoEncontradoException y ConflictoException heredan de 
    * NegocioException, asi que hay que preguntar por las hijas primero. Si se 
    * invierte, todo va a terminar respondiendo 404.
    */
    protected void responderError(HttpServletResponse resp, Exception ex) throws IOException{
        if (ex instanceof ConflictoException) {
            escribirJson(resp, 409, new ApiError("CONFLICTO", ex.getMessage()));
        } else if (ex instanceof NoEncontradoException) {
            escribirJson(resp, 404, new ApiError("NO_ENCONTRADO", ex.getMessage()));
        } else if (ex instanceof NegocioException) {
            escribirJson(resp, 400, new ApiError("SOLICITUD_INVALIDA", ex.getMessage()));
        }else {
            //Error no previsto: el detalle va al log del servidor, Nunca al 
            //cliente. Devolver ex.getMessage() 
            
            ex.printStackTrace();
            escribirJson(resp, 500, new ApiError("ERROR_INTERNO", "Ocurrio un error inesperado en el servidor"));
        }
    }
    
    
} 
