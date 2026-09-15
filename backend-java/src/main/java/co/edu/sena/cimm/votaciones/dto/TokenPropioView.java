package co.edu.sena.cimm.votaciones.dto;

/**
 * Respuesta de POST /api/tokens/generar-propio: el token en claro, una sola
 * vez, igual que TokenGeneradoView para el padron masivo. Es una clase
 * separada y no esa misma porque aqui no hay nombre ni correo que devolver:
 * el estudiante ya sabe quien es, el navegador solo necesita el codigo.
 */
public class TokenPropioView {

    public String token;

    public TokenPropioView(String token) {
        this.token = token;
    }
}
