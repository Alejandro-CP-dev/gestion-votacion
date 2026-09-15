package co.edu.sena.cimm.votaciones.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de POST /api/tokens/generar: el padron completo para entregar.
 */
public class PadronGeneradoView {
 
    public int encuestaId;
    public int totalGenerados;
    public LocalDateTime expiranEn;
    public List<TokenGeneradoView> tokens;
 
    public PadronGeneradoView(int encuestaId, int totalGenerados,
                              LocalDateTime expiranEn, List<TokenGeneradoView> tokens) {
        this.encuestaId = encuestaId;
        this.totalGenerados = totalGenerados;
        this.expiranEn = expiranEn;
        this.tokens = tokens;
    }
}
