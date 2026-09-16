package co.edu.sena.cimm.votaciones.dto;

/**
 * Cuerpo de POST /api/usuarios
 *
 * {
 *   "nombre": "Laura",
 *   "apellido": "Gomez",
 *   "correo": "laura.gomez@sena.edu.co",
 *   "telefono": "3001234567",
 *   "clave": "unaClaveSegura",
 *   "rol": "ESTUDIANTE"
 * }
 *
 * "rol" es opcional: si viene vacio el servicio asume ESTUDIANTE, que es el
 * caso comun (el admin crea admins con mucha menos frecuencia).
 */
public class CrearUsuarioRequest {

    public String nombre;
    public String apellido;
    public String correo;
    public String telefono;
    public String clave;
    public String rol;
}
