package co.edu.sena.cimm.votaciones.dto;

/**
 * Cuerpo de POST /api/tokens/generar-propio
 *
 * {
 *   "encuestaId": 1,
 *   "usuarioId": 3
 * }
 *
 * A diferencia de GenerarTokenRequest (el padron completo que arma el
 * administrador antes de abrir la votacion), este es UN estudiante pidiendo
 * SU propio token, ya con la votacion abierta. El usuarioId lo pone
 * generar_token.php a partir de la sesion PHP, nunca el navegador del
 * estudiante directamente.
 */
public class GenerarTokenPropioRequest {

    public int encuestaId;
    public int usuarioId;
}
