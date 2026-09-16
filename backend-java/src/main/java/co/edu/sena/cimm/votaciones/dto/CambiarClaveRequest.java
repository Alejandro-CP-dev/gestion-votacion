package co.edu.sena.cimm.votaciones.dto;

/**
 * Cuerpo de POST /api/usuarios/{id}/clave
 *
 * Pedir claveActual no es paranoia: sin ella, cualquiera que deje una sesion
 * abierta en un computador compartido permite que otra persona se quede con
 * la cuenta con solo escribir una clave nueva.
 */
public class CambiarClaveRequest {

    public String claveActual;
    public String claveNueva;
}
