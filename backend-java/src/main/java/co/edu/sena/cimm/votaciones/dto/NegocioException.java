package co.edu.sena.cimm.votaciones.dto;

/**
 * Una regla de negocio no se cumplio. Se traduce a HTTP 400 (Bad Request).
 *
 * Ejemplos: "el titulo de la encuesta es obligatorio",
 *           "la opcion no pertenece a esta encuesta".
 *
 * Es RuntimeException a proposito: asi los servicios no tienen que declarar
 * "throws" en cada firma y el codigo de negocio se lee limpio. La captura
 * ocurre en un solo lugar, en BaseApiServlet.
 */
public class NegocioException extends RuntimeException{
    
    public NegocioException(String mensaje){
        super(mensaje);
    }
}
