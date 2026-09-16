package co.edu.sena.cimm.votaciones.dto;

/**
 * Un token recien generado, con el codigo EN CLARO.
 *
 * Esta ya no es la unica forma de ver el codigo en claro: TokenPropioView,
 * devuelto por GET /api/tokens/mio, tambien lo entrega, descifrando la copia
 * que TokenCifradoUtil guarda en TokenOtp.TokenCifrado. Este objeto sigue
 * existiendo aparte porque el administrador necesita el lote completo (con
 * nombre y correo, para imprimir volantes) en una sola respuesta, no
 * consultar estudiante por estudiante.
 */
public class TokenGeneradoView {
    
    public int usuarioId;
    public String nombre;
    public String apellido;
    public String correo;
    public String token;

    public TokenGeneradoView(int usuarioId, String nombre, String apellido, String correo, String token) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.token = token;
    }
    
    
    
}
