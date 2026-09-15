/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.dto;

/**
 *
 * @author Usuario
 */
public class ConflictoException extends NegocioException{
    
    public ConflictoException(String mensaje){
        super(mensaje);
    }
}
