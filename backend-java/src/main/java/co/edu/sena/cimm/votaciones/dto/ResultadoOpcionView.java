package co.edu.sena.cimm.votaciones.dto;

/**
 * El consolidado de una opcion: cuantos votos y que porcentaje.
 *
 * Es el gemelo de OpcionView, y la diferencia es exactamente un campo: aqui SI
 * viaja el conteo. Son dos clases y no una con un campo opcional porque el
 * publico es distinto. Una clase con "conteoVotos" a veces lleno y a veces en
 * cero obliga a recordar, en cada endpoint, cual de los dos casos es. Dos
 * clases hacen que el compilador lo recuerde por ti.
 */
public class ResultadoOpcionView {
    
    public int opcionId;
    public String textoOpcion;
    public int orden;
    public long votos;
    public double porcentaje;
 
    public ResultadoOpcionView(int opcionId, String textoOpcion, int orden,
                               long votos, double porcentaje) {
        this.opcionId = opcionId;
        this.textoOpcion = textoOpcion;
        this.orden = orden;
        this.votos = votos;
        this.porcentaje = porcentaje;
    }
}
