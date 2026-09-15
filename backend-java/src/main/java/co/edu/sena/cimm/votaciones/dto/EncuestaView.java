/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.dto;

import co.edu.sena.cimm.votaciones.model.Encuesta;
import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import co.edu.sena.cimm.votaciones.model.Opcion;
import java.util.ArrayList;
import java.util.List;

/**
 * Una encuesta tal como la ve el votante: con sus opciones, sin conteos.
 */
public class EncuestaView {
    
    public int id;
    public String titulo;
    public String descripcion;
    public EstadoEncuesta estado;
    public List<OpcionView> opciones = new ArrayList<>();
 
    public static EncuestaView desde(Encuesta e) {
        EncuestaView v = new EncuestaView();
        v.id = e.getId();
        v.titulo = e.getTitulo();
        v.descripcion = e.getDescripcion();
        v.estado = e.getEstado();
 
        for (Opcion o : e.getOpcion()) {
            v.opciones.add(OpcionView.desde(o));
        }
        return v;
    }
 
    public static List<EncuestaView> desde(List<Encuesta> encuestas) {
        List<EncuestaView> lista = new ArrayList<>();
        for (Encuesta e : encuestas) {
            lista.add(desde(e));
        }
        return lista;
    }
}
