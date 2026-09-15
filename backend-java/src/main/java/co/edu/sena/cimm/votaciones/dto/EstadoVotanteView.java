package co.edu.sena.cimm.votaciones.dto;

/**
 * Respuesta de GET /api/tokens/estado?encuestaId=..&usuarioId=..
 *
 * Lo consume votar.php para mostrar la encuesta bloqueada si la persona ya
 * voto. Devuelve QUE voto, jamas POR QUE opcion: esa informacion no existe en
 * ninguna tabla del sistema.
 */
public class EstadoVotanteView {
 
    public int encuestaId;
    public int usuarioId;
    public boolean tieneToken;
    public boolean yaVoto;
 
    public EstadoVotanteView(int encuestaId, int usuarioId, boolean tieneToken, boolean yaVoto) {
        this.encuestaId = encuestaId;
        this.usuarioId = usuarioId;
        this.tieneToken = tieneToken;
        this.yaVoto = yaVoto;
    }
}
