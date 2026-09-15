package co.edu.sena.cimm.votaciones.service;

import co.edu.sena.cimm.votaciones.dto.ComprobanteView;
import co.edu.sena.cimm.votaciones.dto.EmitirVotoRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.repository.VotoRepository;
import co.edu.sena.cimm.votaciones.util.TokenUtil;

/**
 * Emision del voto.
 *
 * Esta clase es corta a proposito. Toda la logica que importa vive dentro de la
 * transaccion, y la transaccion vive en el repositorio. Intentar validar aqui
 * si el token esta disponible seria peor que inutil: entre esa verificacion y
 * la transaccion cabe otra peticion, y quedaria una comprobacion que da falsa
 * tranquilidad.
 *
 * La regla general: lo que se valida contra el estado de la base de datos se
 * valida DENTRO de la transaccion que lo va a modificar. Aqui solo queda lo que
 * se puede verificar mirando la peticion.
 */
public class VotoService {

    private final VotoRepository votoRepository;

    public VotoService(VotoRepository votoRepository) {
        this.votoRepository = votoRepository;
    }

    public ComprobanteView emitir(EmitirVotoRequest peticion) {

        if (peticion == null || peticion.token == null || peticion.token.trim().isEmpty()) {
            throw new NegocioException("Debe ingresar su token de votacion");
        }
        if (peticion.opcionId <= 0) {
            throw new NegocioException("Debe seleccionar una opcion");
        }

        // Aqui el token en claro se convierte en hash y deja de existir.
        //
        // NUNCA registrar esta variable en un log. Un System.out.println con el
        // token, sumado a la hora de la peticion, permitiria reconstruir quien
        // voto y cuando. Seria la unica grieta del sistema, y estaria en una
        // linea puesta para depurar y olvidada ahi.
        String tokenHash = TokenUtil.hashDeToken(peticion.token);

        if (tokenHash.isEmpty()) {
            throw new NegocioException("El token no es valido");
        }

        String codigoRecibo = votoRepository.emitir(tokenHash, peticion.opcionId);

        return new ComprobanteView(codigoRecibo, "Su voto fue registrado. Guarde este codigo como comprobante");
    }
}
