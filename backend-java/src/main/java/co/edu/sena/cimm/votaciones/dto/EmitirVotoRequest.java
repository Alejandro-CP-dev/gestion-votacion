package co.edu.sena.cimm.votaciones.dto;

/**
 * Cuerpo de POST /api/votos/emitir
 *
 * {
 * "token": "ABCDE-FGHJK", "opcionId": 7 }
 *
 * MIRA LO QUE NO ESTA AQUI: no hay usuarioId, ni correo, ni nada que
 * identifique a la persona. El token ES la credencial.
 *
 * Y no es que el backend se abstenga de usar esa informacion: es que nunca la
 * recibe. No se puede filtrar lo que no llego. Cuando en la sustentacion
 * pregunten como se garantiza el anonimato, esta clase de cuatro lineas es
 * mejor respuesta que cualquier explicacion.
 */
public class EmitirVotoRequest {

    public String token;
    public int opcionId;
}
