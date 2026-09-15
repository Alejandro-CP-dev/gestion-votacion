/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Una encuesta o votacion, junto con sus opciones.
 *
 * La lista de opciones vive DENTRO de la encuesta porque nunca se consulta ni
 * se guarda por separado: se crean en la misma transaccion y se leen en la
 * misma consulta. Ese vinculo es el que hace que EncuestaRepository sea dueno
 * de las dos tablas y que Opcion no necesite repositorio propio.
 *
 * La lista se inicializa vacia para que nunca sea null. Un metodo que recorre
 * encuesta.getOpciones() no deberia tener que preguntar primero si existe.
 */
public class Encuesta {
    
    private int Id;
    private String Titulo;
    private String Descripcion;
    private EstadoEncuesta estado;
    private int CreadoPor;
    private LocalDateTime creadoEn;
    private List<Opcion> Opcion = new ArrayList<>();

    public Encuesta() {
    }

    public Encuesta(int Id, String Titulo, String Descripcion, EstadoEncuesta estado, int CreadoPor, LocalDateTime creadoEn) {
        this.Id = Id;
        this.Titulo = Titulo;
        this.Descripcion = Descripcion;
        this.estado = estado;
        this.CreadoPor = CreadoPor;
        this.creadoEn = creadoEn;
    }

    public int getId() {
        return Id;
    }

    public void setId(int Id) {
        this.Id = Id;
    }

    public String getTitulo() {
        return Titulo;
    }

    public void setTitulo(String Titulo) {
        this.Titulo = Titulo;
    }

    public String getDescripcion() {
        return Descripcion;
    }

    public void setDescripcion(String Descripcion) {
        this.Descripcion = Descripcion;
    }

    public EstadoEncuesta getEstado() {
        return estado;
    }

    public void setEstado(EstadoEncuesta estado) {
        this.estado = estado;
    }

    public int getCreadoPor() {
        return CreadoPor;
    }

    public void setCreadoPor(int CreadoPor) {
        this.CreadoPor = CreadoPor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public List<Opcion> getOpcion() {
        return Opcion;
    }

    public void setOpcion(List<Opcion> Opcion) {
        this.Opcion = Opcion;
    }
    
    
}
