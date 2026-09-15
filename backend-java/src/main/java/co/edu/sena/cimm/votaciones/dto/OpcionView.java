/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.dto;

import co.edu.sena.cimm.votaciones.model.Opcion;

/**
 * Una opcion tal como la ve el VOTANTE.
 *
 * NO TIENE conteoVotos, y esa ausencia es deliberada.
 *
 * Si el conteo viajara hasta la papeleta, cualquiera abriria la consola del
 * navegador y veria el resultado parcial en vivo. Saber quien va ganando antes
 * de votar cambia el voto de mucha gente. No viola ninguna de las cuatro
 * reglas del taller y aun asi arruina la eleccion.
 *
 * El consolidado con conteos existira aparte, en el paso 7, y solo para el
 * administrador.
 */
public class OpcionView {
    
    public int id;
    public String textoOpcion;
    public int orden;
 
    public OpcionView(int id, String textoOpcion, int orden) {
        this.id = id;
        this.textoOpcion = textoOpcion;
        this.orden = orden;
    }
 
    public static OpcionView desde(Opcion o) {
        return new OpcionView(o.getId(), o.getTextoOpcion(), o.getOrden());
    }
}
