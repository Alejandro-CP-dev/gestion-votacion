package co.edu.sena.cimm.votaciones.dto;

import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import java.util.List;

/**
 * Consolidado de una votacion: conteos, porcentajes y participacion.
 *
 * NO TIENE UN CAMPO "ganador", y es deliberado. Declarar un ganador exige
 * decidir que pasa con un empate, si el voto en blanco compite y con que umbral
 * se gana. Esas son reglas del reglamento estudiantil, no del software. Un
 * sistema que las resuelve en silencio le esconde a la institucion una decision
 * que le corresponde a ella.
 *
 * "participacion" es el dato que si se puede mostrar durante una votacion
 * abierta sin influir en nadie: cuanta gente ha votado, no por quien.
 */
public class ResultadoEncuestaView {

    public int encuestaId;
    public String titulo;
    public EstadoEncuesta estado;
    public long totalVotos;
    public int habilitados;
    public double participacion;
    public List<ResultadoOpcionView> opciones;

    public ResultadoEncuestaView(int encuestaId, String titulo, EstadoEncuesta estado,
            long totalVotos, int habilitados, double participacion,
            List<ResultadoOpcionView> opciones) {
        this.encuestaId = encuestaId;
        this.titulo = titulo;
        this.estado = estado;
        this.totalVotos = totalVotos;
        this.habilitados = habilitados;
        this.participacion = participacion;
        this.opciones = opciones;
    }
}
