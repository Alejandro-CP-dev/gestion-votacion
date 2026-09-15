package co.edu.sena.cimm.votaciones.dto;

/**
 * Cuerpo de POST /api/tokens/generar
 *
 * {
 *   "encuestaId": 1,
 *   "horasVigencia": 24
 * }
 *
 * horasVigencia es el TTL que pide el taller. Si llega en 0 o ausente, el
 * servicio aplica 24 horas.
 */
public class GenerarTokenRequest {
    
    public int encuestaId;
    public int horasVigencia;
}
