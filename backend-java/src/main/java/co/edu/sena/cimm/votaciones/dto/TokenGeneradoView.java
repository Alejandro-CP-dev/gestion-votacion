package co.edu.sena.cimm.votaciones.dto;

/**
 * Un token recien generado, con el codigo EN CLARO.
 *
 * Este es el unico objeto del proyecto que lleva un token legible, y existe
 * durante una sola respuesta HTTP. Despues de eso el codigo original no esta
 * en ninguna parte: la base de datos solo guarda su SHA-256, que no se puede
 * revertir.
 *
 * Consecuencia practica que el administrador debe entender: si pierde esta
 * lista, no hay forma de recuperarla. Se anula el padron y se genera otro.
 * Eso no es un defecto, es la propiedad que hace que una filtracion de la base
 * de datos no le permita a nadie votar en nombre de otro.
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
