package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.model.TokenOtp;
import java.util.List;

/**
 * Acceso a la tabla TokenOtp para la generacion del padron.
 *
 * OJO: aqui NO hay un metodo marcarUsado(). Quemar un token solo tiene sentido
 * dentro de la transaccion del voto, asi que esa operacion vive en
 * VotoRepository. Si existiera aqui, alguien la llamaria por fuera de la
 * transaccion y la condicion de carrera volveria por la puerta de atras.
 */
public interface TokenRepository {

    /**
     * Inserta el padron completo de tokens en UNA transaccion.
     *
     * Todo o nada: si el token 40 de 120 choca con el UNIQUE(EncuestaId,
     * UsuarioId), se revierten los 39 anteriores. Un padron a medias dejaria a
     * unos estudiantes con credencial y a otros sin ella, y no habria forma
     * limpia de saber a quienes reintentar.
     *
     * @return cuantos tokens quedaron guardados.
     */
    int guardarPadron(List<TokenOtp> tokens);
 
    /** Evita que el administrador genere el padron dos veces por error. */
    boolean existePadron(int encuestaId);
 
    /** Si el usuario esta habilitado para votar en esta encuesta. */
    boolean tieneToken(int encuestaId, int usuarioId);
 
    /** Cuantas personas quedaron habilitadas: el tamano del padron. */
    int contarHabilitados(int encuestaId);
 
    /**
     * Cuantas personas ya votaron (tokens en estado USADO).
     *
     * Es un conteo agregado: dice cuantos sufragios se emitieron, no quienes
     * los emitieron ni por que opcion. Sirve para la participacion sin tocar
     * el secreto del voto.
     */
    int contarVotantes(int encuestaId);
 
    /**
     * Si el usuario ya voto en esta encuesta.
     *
     * Este metodo revela QUE la persona voto, nunca POR QUE opcion. Esa
     * distincion es la que permite cumplir a la vez el control del sufragio
     * (Regla 1) y el secreto del sufragio (Regla 4). Lo necesita votar.php
     * para mostrar la encuesta bloqueada.
     */
    boolean usuarioYaVoto(int encuestaId, int usuarioId);
}
