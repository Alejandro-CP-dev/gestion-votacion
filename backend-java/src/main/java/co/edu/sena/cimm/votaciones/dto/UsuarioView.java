package co.edu.sena.cimm.votaciones.dto;

import co.edu.sena.cimm.votaciones.model.RolUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;

/**
 * El perfil que se devuelve tras un login exitoso. Es lo que el frontend
 * guarda en $_SESSION.
 *
 * ESTA CLASE EXISTE POR UNA SOLA RAZON: no tiene el campo "clave".
 *
 * Si el servlet devolviera el objeto Usuario directamente, Gson serializaria
 * TODOS sus campos, incluido el hash BCrypt, y ese hash viajaria por la red
 * hasta el navegador en cada login. Nadie lo notaria: el JSON funciona igual
 * y la aplicacion se ve bien.
 *
 * Esa es la respuesta a "por que un DTO si ya tengo el modelo": el modelo
 * refleja lo que la base de datos guarda, el DTO decide lo que el mundo ve.
 * Son dos decisiones distintas y no tienen por que coincidir.
 */
public class UsuarioView {
    
    public int id;
    public String nombre;
    public String apellido;
    public String correo;
    public RolUsuario rol;

    public UsuarioView(int id, String nombre, String apellido, String correo, RolUsuario rol) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.rol = rol;
    }
    
    /**
     * Traduce del modelo al DTO. Vive aqui y no en el servicio para que la
     * conversion este en un solo lugar: si manana se agrega un campo, se
     * agrega aqui y aparece en todos los endpoints que devuelven un perfil.
     */
    public static UsuarioView desde(Usuario u) {
        return new UsuarioView(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(), u.getRol());
    }
}
