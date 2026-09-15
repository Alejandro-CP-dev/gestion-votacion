package co.edu.sena.cimm.votaciones.dto;

/**
 *
 * @author Usuario
 */
public class NoEncontradoException extends NegocioException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
