package co.edu.sena.cimm.votaciones.service;

import co.edu.sena.cimm.votaciones.dto.ConflictoException;
import co.edu.sena.cimm.votaciones.dto.EstadoVotanteView;
import co.edu.sena.cimm.votaciones.dto.GenerarTokenPropioRequest;
import co.edu.sena.cimm.votaciones.dto.GenerarTokenRequest;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.dto.NoEncontradoException;
import co.edu.sena.cimm.votaciones.dto.PadronGeneradoView;
import co.edu.sena.cimm.votaciones.dto.TokenGeneradoView;
import co.edu.sena.cimm.votaciones.dto.TokenPropioView;
import co.edu.sena.cimm.votaciones.model.Encuesta;
import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import co.edu.sena.cimm.votaciones.model.EstadoToken;
import co.edu.sena.cimm.votaciones.model.RolUsuario;
import co.edu.sena.cimm.votaciones.model.TokenOtp;
import co.edu.sena.cimm.votaciones.model.Usuario;
import co.edu.sena.cimm.votaciones.repository.EncuestaRepository;
import co.edu.sena.cimm.votaciones.repository.TokenRepository;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import co.edu.sena.cimm.votaciones.util.TokenCifradoUtil;
import co.edu.sena.cimm.votaciones.util.TokenUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Generacion del padron electoral: un token unico por estudiante y encuesta.
 */
public class TokenService {

    private static final int HORAS_POR_DEFECTO = 24;
    private static final int HORAS_MAXIMO = 720; // 30 dias

    private final TokenRepository tokenRepo;
    private final EncuestaRepository encuestaRepo;
    private final UsuarioRepository usuarioRepo;

    public TokenService(TokenRepository tokenRepo, EncuestaRepository encuestaRepo, UsuarioRepository usuarioRepo) {
        this.tokenRepo = tokenRepo;
        this.encuestaRepo = encuestaRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public PadronGeneradoView generarPadron(GenerarTokenRequest peticion) {

        if (peticion == null || peticion.encuestaId <= 0) {
            throw new NegocioException("El id de la encuesta es obligatorio");
        }

        int horas = (peticion.horasVigencia <= 0) ? HORAS_POR_DEFECTO : peticion.horasVigencia;

        if (horas > HORAS_MAXIMO) {
            throw new NegocioException("La vigencia no puede superar las " + HORAS_MAXIMO + " horas");
        }

        Encuesta encuesta = encuestaRepo.buscarPorId(peticion.encuestaId).orElseThrow(() -> new NoEncontradoException("No existe la encuesta " + peticion.encuestaId));

        // El padron se congela ANTES de abrir la votacion.
        //
        // Si se pudieran emitir tokens con la encuesta ya ACTIVA, el
        // administrador podria agregar votantes a mitad de la eleccion, viendo
        // ya por donde va el conteo. Es una regla de integridad del proceso
        // democratico, no una limitacion tecnica.
        if (encuesta.getEstado() != EstadoEncuesta.CREADA) {
            throw new NegocioException("El padron solo se genera con la encuesta en estado CREADA. "
                    + "Estado actual: " + encuesta.getEstado());
        }

        // Verificacion amable. La garantia real es el UNIQUE de la base de
        // datos: entre este if y el INSERT cabe otra peticion identica.
        if (tokenRepo.existePadron(encuesta.getId())) {
            throw new ConflictoException("Esta encuesta ya tiene su padron generado");
        }

        List<Usuario> votantes = usuarioRepo.listarEstudiantesActivos();
        if (votantes.isEmpty()) {
            throw new NegocioException("No hay estudiantes activos para generar el padron");
        }

        LocalDateTime expiracion = LocalDateTime.now().plusHours(horas);

        List<TokenOtp> paraGuardar = new ArrayList<>();
        List<TokenGeneradoView> paraEntregar = new ArrayList<>();

        for (Usuario votante : votantes) {
            String tokenEnClaro = TokenUtil.generarToken();

            TokenOtp t = new TokenOtp();
            t.setEncuestaId(encuesta.getId());
            t.setUsuarioId(votante.getId());
            t.setTokenHash(TokenUtil.hashDeToken(tokenEnClaro));
            t.setTokenCifrado(TokenCifradoUtil.cifrar(tokenEnClaro));
            t.setEstado(EstadoToken.DISPONIBLE);
            t.setFechaExpiracion(expiracion);
            paraGuardar.add(t);

            paraEntregar.add(new TokenGeneradoView(
                    votante.getId(), votante.getNombre(), votante.getApellido(),
                    votante.getCorreo(), tokenEnClaro));
        }

        int guardados = tokenRepo.guardarPadron(paraGuardar);

        // A partir de aqui los tokens en claro solo viven en la respuesta HTTP.
        // La base de datos tiene unicamente sus hashes.
        return new PadronGeneradoView(encuesta.getId(), guardados, expiracion, paraEntregar);
    }

    /**
     * Un estudiante genera SU PROPIO token para una votacion que ya esta
     * abierta, sin esperar a que el administrador arme un padron masivo.
     *
     * Diferencias a proposito con generarPadron():
     *
     *   - Exige la encuesta ACTIVA, no CREADA: el padron masivo se genera
     *     ANTES de abrir para congelar la lista de votantes; aqui la votacion
     *     ya esta corriendo y cada quien pide el suyo cuando le toca votar.
     *   - Un solo TokenOtp, no una lista: se reutiliza guardarPadron() con
     *     una lista de un elemento en vez de duplicar el INSERT.
     *   - La garantia real contra pedir dos veces sigue siendo el UNIQUE
     *     (EncuestaId, UsuarioId) de la base de datos; tieneToken() aqui es
     *     la misma verificacion amable que en el padron masivo.
     */
    public TokenPropioView generarTokenPropio(GenerarTokenPropioRequest peticion) {

        if (peticion == null || peticion.encuestaId <= 0 || peticion.usuarioId <= 0) {
            throw new NegocioException("encuestaId y usuarioId son obligatorios");
        }

        Encuesta encuesta = encuestaRepo.buscarPorId(peticion.encuestaId)
                .orElseThrow(() -> new NoEncontradoException("No existe la encuesta " + peticion.encuestaId));

        if (encuesta.getEstado() != EstadoEncuesta.ACTIVA) {
            throw new NegocioException("Esta votacion no esta abierta");
        }

        Usuario usuario = usuarioRepo.buscarPorId(peticion.usuarioId)
                .orElseThrow(() -> new NoEncontradoException("No existe el usuario " + peticion.usuarioId));

        if (usuario.getRol() != RolUsuario.ESTUDIANTE) {
            throw new NegocioException("Solo un estudiante puede generar su propio token");
        }

        if (tokenRepo.tieneToken(peticion.encuestaId, peticion.usuarioId)) {
            throw new ConflictoException("Ya tienes un token para esta votacion");
        }

        String tokenEnClaro = TokenUtil.generarToken();

        TokenOtp t = new TokenOtp();
        t.setEncuestaId(peticion.encuestaId);
        t.setUsuarioId(peticion.usuarioId);
        t.setTokenHash(TokenUtil.hashDeToken(tokenEnClaro));
        t.setTokenCifrado(TokenCifradoUtil.cifrar(tokenEnClaro));
        t.setEstado(EstadoToken.DISPONIBLE);
        t.setFechaExpiracion(LocalDateTime.now().plusHours(HORAS_POR_DEFECTO));

        // Mismo camino que el padron masivo: si el UNIQUE choca (dos
        // pestañas pidiendo a la vez), la base de datos es quien lo impide.
        tokenRepo.guardarPadron(Collections.singletonList(t));

        return new TokenPropioView(tokenEnClaro);
    }

    /**
     * El token que el aprendiz ya tiene asignado (del padron del
     * administrador o de su propio generarTokenPropio), para mostrarselo de
     * nuevo en su panel al elegir la encuesta.
     *
     * No genera nada nuevo: si no hay un token DISPONIBLE (nunca tuvo, ya
     * voto o expiro), NoEncontradoException. votar.php/papeleta.php ya saben
     * distinguir esos casos con /tokens/estado antes de llamar aqui.
     */
    public TokenPropioView obtenerMiToken(int encuestaId, int usuarioId) {
        if (encuestaId <= 0 || usuarioId <= 0) {
            throw new NegocioException("encuestaId y usuarioId son obligatorios");
        }

        Optional<String> cifrado = tokenRepo.obtenerTokenCifrado(encuestaId, usuarioId);

        String tokenCifrado = cifrado.orElseThrow(
                () -> new NoEncontradoException("No tienes un token disponible para esta votacion"));

        return new TokenPropioView(TokenCifradoUtil.descifrar(tokenCifrado));
    }

    public EstadoVotanteView consultarEstado(int encuestaId, int usuarioId) {
        if (encuestaId <= 0 || usuarioId <= 0) {
            throw new NegocioException("encuestaId y usuarioId son obligatorios");
        }

        boolean tiene = tokenRepo.tieneToken(encuestaId, usuarioId);
        boolean voto = tiene && tokenRepo.usuarioYaVoto(encuestaId, usuarioId);

        return new EstadoVotanteView(encuestaId, usuarioId, tiene, voto);
    }
}
