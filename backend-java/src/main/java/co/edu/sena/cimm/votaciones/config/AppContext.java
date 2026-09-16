package co.edu.sena.cimm.votaciones.config;

import co.edu.sena.cimm.votaciones.repository.EncuestaRepository;
import co.edu.sena.cimm.votaciones.repository.JdbcEncuestaRepository;
import co.edu.sena.cimm.votaciones.repository.JdbcTokenRepository;
import co.edu.sena.cimm.votaciones.repository.JdbcUsuarioRepository;
import co.edu.sena.cimm.votaciones.repository.JdbcVotoRepository;
import co.edu.sena.cimm.votaciones.repository.TokenRepository;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import co.edu.sena.cimm.votaciones.repository.VotoRepository;
import co.edu.sena.cimm.votaciones.service.AuthService;
import co.edu.sena.cimm.votaciones.service.EncuestaService;
import co.edu.sena.cimm.votaciones.service.TokenService;
import co.edu.sena.cimm.votaciones.service.UsuarioService;
import co.edu.sena.cimm.votaciones.service.VotoService;

/**
 * El unico lugar donde la aplicacion decide QUE implementacion usa.
 *
 * Es inyeccion de dependencias a mano. Podria hacerse con CDI o Spring, pero
 * para este tamano de proyecto son 200 KB de framework y una capa de magia para
 * resolver lo que aqui se resuelve en cinco lineas que cualquiera puede leer de
 * arriba a abajo.
 *
 * El valor esta en que sea UN solo lugar. Cambiar JdbcEncuestaRepository por
 * una version en memoria es editar una linea, y ningun servicio ni servlet se
 * entera. Si cada servlet hiciera su propio "new", ese cambio serian diez
 * ediciones y una que se olvida.
 *
 * Los campos son static final: se construyen una vez cuando Tomcat carga la
 * clase y viven mientras viva la aplicacion. Servicios y repositorios no
 * guardan estado entre peticiones, asi que compartirlos es seguro incluso con
 * peticiones concurrentes.
 */
public final class AppContext {
    // ---- Repositorios ----

    private static final UsuarioRepository USUARIO_REPOSITORY = new JdbcUsuarioRepository();
    private static final EncuestaRepository ENCUESTA_REPOSITORY = new JdbcEncuestaRepository();
    private static final TokenRepository TOKEN_REPOSITORY = new JdbcTokenRepository();
    private static final VotoRepository VOTO_REPOSITORY = new JdbcVotoRepository();

    // ---- Servicios ----
    private static final AuthService AUTH_SERVICE = new AuthService(USUARIO_REPOSITORY);
    private static final EncuestaService ENCUESTA_SERVICE = new EncuestaService(ENCUESTA_REPOSITORY, USUARIO_REPOSITORY, TOKEN_REPOSITORY);
    private static final TokenService TOKEN_SERVICE = new TokenService(TOKEN_REPOSITORY, ENCUESTA_REPOSITORY, USUARIO_REPOSITORY);
    private static final VotoService VOTO_SERVICE = new VotoService(VOTO_REPOSITORY);
    private static final UsuarioService USUARIO_SERVICE = new UsuarioService(USUARIO_REPOSITORY);

    private AppContext() {
    }

    public static AuthService getAuthService() {
        return AUTH_SERVICE;
    }

    public static EncuestaService getEncuestaService() {
        return ENCUESTA_SERVICE;
    }

    public static TokenService getTokenService() {
        return TOKEN_SERVICE;
    }

    public static VotoService getVotoService() {
        return VOTO_SERVICE;
    }

    public static UsuarioRepository getUsuarioRepository() {
        return USUARIO_REPOSITORY;
    }

    public static UsuarioService getUsuarioService() {
        return USUARIO_SERVICE;
    }

    public static EncuestaRepository getEncuestaRepository() {
        return ENCUESTA_REPOSITORY;
    }

    public static TokenRepository getTokenRepository() {
        return TOKEN_REPOSITORY;
    }
}
