/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.model;

/**
 * Una opcion de voto dentro de una encuesta.
 *
 * "conteoVotos" es el unico registro de votos que existe en todo el sistema.
 * No hay tabla Voto, no hay lista de votantes, no hay historial. Esa ausencia
 * ES la REGLA 4: lo que no se guarda no se puede filtrar ni rastrear.
 *
 * Nota: esta clase no tiene metodo para incrementar el contador. Sumar un voto
 * no es una operacion del objeto en memoria, es una operacion de la base de
 * datos dentro de una transaccion (UPDATE ... SET ConteoVotos = ConteoVotos+1).
 * Si existiera un opcion.sumarVoto(), tarde o temprano alguien lo llamaria
 * fuera de la transaccion y tendriamos la condicion de carrera de vuelta.
 */
public class Opcion {
    
    private int Id;
    private int EncuestaId;
    private String TextoOpcion;
    private int Orden;
    private long conteoVotos;

    public Opcion() {
    }

    public Opcion(int Id, int EncuestaId, String TextoOpcion, int Orden, long conteoVotos) {
        this.Id = Id;
        this.EncuestaId = EncuestaId;
        this.TextoOpcion = TextoOpcion;
        this.Orden = Orden;
        this.conteoVotos = conteoVotos;
    }

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }

    public int getEncuestaId() {
        return EncuestaId;
    }

    public void setEncuestaId(int EncuestaId) {
        this.EncuestaId = EncuestaId;
    }

    public String getTextoOpcion() {
        return TextoOpcion;
    }

    public void setTextoOpcion(String TextoOpcion) {
        this.TextoOpcion = TextoOpcion;
    }

    public int getOrden() {
        return Orden;
    }

    public void setOrden(int Orden) {
        this.Orden = Orden;
    }

    public long getConteoVotos() {
        return conteoVotos;
    }

    public void setConteoVotos(long conteoVotos) {
        this.conteoVotos = conteoVotos;
    }
    
    
}
