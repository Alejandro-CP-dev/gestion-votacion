package co.edu.sena.cimm.votaciones.dto;

/**
 * Cuerpo de PUT /api/usuarios/{id}/perfil
 *
 * Solo los datos que el propio usuario puede editar de si mismo. Ni correo
 * ni rol ni estado viajan aqui: cambiarlos es una decision del administrador,
 * no del autoservicio.
 */
public class ActualizarPerfilRequest {

    public String nombre;
    public String apellido;
    public String telefono;
}
