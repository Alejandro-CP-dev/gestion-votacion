package co.edu.sena.cimm.votaciones.dto;

/**
 * El recibo anonimo que recibe el votante.
 *
 * NO devuelve la opcion elegida, aunque el votante obviamente la conozca. La
 * razon es practica: esta respuesta pasa por el frontend PHP, y los logs de
 * acceso, las herramientas de depuracion y los proxys guardan cuerpos de
 * respuesta con mas frecuencia de la que uno cree. Si la opcion no viaja, no
 * hay donde quede registrada por accidente.
 *
 * Tampoco devuelve el id de la encuesta: el frontend ya sabe en cual estaba, y
 * un DTO no deberia cargar campos que nadie usa.
 *
 * El codigo de recibo es el SHA-256 de un UUID aleatorio generado en el momento
 * del voto. Deliberadamente NO se usa el hash del token: si se usara, el codigo
 * del comprobante seria identico al TokenHash de la tabla TokenOtp, y
 * cualquiera podria cruzar las dos tablas para saber que recibo le corresponde
 * a que persona.
 */
public class ComprobanteView {

    public String codigoRecibo;
    public String mensaje;

    public ComprobanteView(String codigoRecibo, String mensaje) {
        this.codigoRecibo = codigoRecibo;
        this.mensaje = mensaje;
    }

}
