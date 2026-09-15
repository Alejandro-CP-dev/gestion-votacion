/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.service;

import co.edu.sena.cimm.votaciones.dto.ConflictoException;
import co.edu.sena.cimm.votaciones.dto.CrearEncuestaRequest;
import co.edu.sena.cimm.votaciones.dto.EncuestaView;
import co.edu.sena.cimm.votaciones.dto.NegocioException;
import co.edu.sena.cimm.votaciones.dto.NoEncontradoException;
import co.edu.sena.cimm.votaciones.dto.ResultadoEncuestaView;
import co.edu.sena.cimm.votaciones.dto.ResultadoOpcionView;
import co.edu.sena.cimm.votaciones.model.Encuesta;
import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import co.edu.sena.cimm.votaciones.model.Opcion;
import co.edu.sena.cimm.votaciones.model.RolUsuario;
import co.edu.sena.cimm.votaciones.model.Usuario;
import co.edu.sena.cimm.votaciones.repository.EncuestaRepository;
import co.edu.sena.cimm.votaciones.repository.TokenRepository;
import co.edu.sena.cimm.votaciones.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reglas de negocio de la gestion electoral.
 *
 * Todo lo que esta clase decide es "que esta permitido". El repositorio decide
 * "como se guarda". Ninguna de las dos invade a la otra.
 */
public class EncuestaService {

    private static final int MINIMO_OPCIONES = 2;
    private static final int MAXIMO_LARGO_TITULO = 200;

    private final EncuestaRepository encuestaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenRepository tokenRepository;

    public EncuestaService(EncuestaRepository encuestaRepository,
                           UsuarioRepository usuarioRepository,
                           TokenRepository tokenRepository) {
        this.encuestaRepository = encuestaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
    }

    public EncuestaView crear(CrearEncuestaRequest peticion) {

        if (peticion == null) {
            throw new NegocioException("El cuerpo de la peticion es obligatorio");
        }
        if (peticion.titulo == null || peticion.titulo.trim().isEmpty()) {
            throw new NegocioException("El titulo de la encuesta es obligatorio");
        }
        if (peticion.titulo.trim().length() > MAXIMO_LARGO_TITULO) {
            throw new NegocioException(
                    "El titulo no puede superar los " + MAXIMO_LARGO_TITULO + " caracteres");
        }

        // El rol se verifica en el backend aunque el frontend ya lo haya hecho.
        // Cualquier validacion que solo exista en el cliente se puede saltar.
        Optional<Usuario> autor = usuarioRepository.buscarPorId(peticion.creadoPor);
        if (!autor.isPresent()) {
            throw new NegocioException("El usuario que crea la encuesta no existe");
        }
        if (autor.get().getRol() != RolUsuario.ADMIN) {
            throw new NegocioException("Solo un administrador puede crear encuestas");
        }

        List<String> opciones = limpiarOpciones(peticion.opciones);

        if (opciones.size() < MINIMO_OPCIONES) {
            throw new NegocioException(
                    "La encuesta debe tener al menos " + MINIMO_OPCIONES + " opciones distintas");
        }

        Encuesta encuesta = new Encuesta();
        encuesta.setTitulo(peticion.titulo.trim());
        encuesta.setDescripcion(peticion.descripcion == null ? null : peticion.descripcion.trim());
        encuesta.setCreadoPor(peticion.creadoPor);

        List<Opcion> lista = new ArrayList<>();
        for (String texto : opciones) {
            Opcion o = new Opcion();
            o.setTextoOpcion(texto);
            lista.add(o);
        }
        encuesta.setOpcion(lista);

        return EncuestaView.desde(encuestaRepository.crear(encuesta));
    }

    public EncuestaView buscarPorId(int id) {
        Encuesta e = encuestaRepository.buscarPorId(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la encuesta " + id));
        return EncuestaView.desde(e);
    }

    public List<EncuestaView> listarActivas() {
        return EncuestaView.desde(encuestaRepository.listarPorEstado(EstadoEncuesta.ACTIVA));
    }

    /**
     * Listado para el panel del administrador.
     *
     * Con estado en null devuelve todas. El admin necesita ver las CREADA para
     * generarles padron y las CERRADA para consultar resultados, no solo las
     * que estan recibiendo votos.
     */
    public List<EncuestaView> listar(EstadoEncuesta estado) {
        if (estado == null) {
            return EncuestaView.desde(encuestaRepository.listarTodas());
        }
        return EncuestaView.desde(encuestaRepository.listarPorEstado(estado));
    }
    
    
    /**
     * La maquina de estados de la votacion.
     *
     * CREADA -> ACTIVA se abre la votacion ACTIVA -> CERRADA se cierra la
     * votacion
     *
     * Cualquier otra transicion se rechaza, y CERRADA no tiene salida. Reabrir
     * una votacion cerrada permitiria seguir sumando votos despues de haber
     * publicado el resultado, y ahi se acaba la confianza en el sistema
     * completo. Es una regla de negocio, no un detalle tecnico, y por eso vive
     * aqui y no en el repositorio.
     */
    public EncuestaView cambiarEstado(int encuestaId, EstadoEncuesta nuevoEstado) {

        if (nuevoEstado == null) {
            throw new NegocioException(
                    "El estado es obligatorio y debe ser CREADA, ACTIVA o CERRADA");
        }

        Encuesta actual = encuestaRepository.buscarPorId(encuestaId)
                .orElseThrow(() -> new NoEncontradoException("No existe la encuesta " + encuestaId));

        if (!transicionValida(actual.getEstado(), nuevoEstado)) {
            throw new NegocioException("No se puede pasar de "
                    + actual.getEstado() + " a " + nuevoEstado);
        }

        boolean aplicado = encuestaRepository.cambiarEstado(
                encuestaId, actual.getEstado(), nuevoEstado);

        // false significa que entre la lectura y la escritura alguien mas
        // cambio el estado. No es un error del cliente: es una carrera perdida.
        if (!aplicado) {
            throw new ConflictoException("Otro administrador modifico esta encuesta. Vuelva a consultarla");
        }

        return buscarPorId(encuestaId);
    }

    
    /**
     * Consolidado de la votacion: conteos, porcentajes y participacion.
     *
     * SOBRE LOS PORCENTAJES: se redondean a un decimal, y por eso la suma
     * puede dar 99.9 o 100.1. Es inevitable al redondear (3 opciones con 1
     * voto cada una dan 33.3 tres veces, que suman 99.9). Lo correcto es
     * conocerlo y decirlo, no maquillar el ultimo valor para que cuadre: los
     * numeros que se muestran deben ser los que salen de la division.
     *
     * El dato incuestionable es "votos". El porcentaje es una ayuda visual.
     */
    public ResultadoEncuestaView resultados(int encuestaId) {
 
        Encuesta encuesta = encuestaRepository.buscarPorId(encuestaId)
                .orElseThrow(() -> new NoEncontradoException("No existe la encuesta " + encuestaId));
 
        long totalVotos = 0;
        for (Opcion o : encuesta.getOpcion()) {
            totalVotos += o.getConteoVotos();
        }
 
        List<ResultadoOpcionView> detalle = new ArrayList<>();
        for (Opcion o : encuesta.getOpcion()) {
            detalle.add(new ResultadoOpcionView(
                    o.getId(),
                    o.getTextoOpcion(),
                    o.getOrden(),
                    o.getConteoVotos(),
                    porcentaje(o.getConteoVotos(), totalVotos)));
        }
 
        int habilitados = tokenRepository.contarHabilitados(encuestaId);
        int votantes = tokenRepository.contarVotantes(encuestaId);
 
        return new ResultadoEncuestaView(
                encuesta.getId(),
                encuesta.getTitulo(),
                encuesta.getEstado(),
                totalVotos,
                habilitados,
                porcentaje(votantes, habilitados),
                detalle);
    }
    
    
    // ==================== apoyo ====================
    /**
     * Antes de dividir siempre se pregunta por el cero. Con una encuesta recien
     * abierta el total es 0, y en Java la division entera por cero lanza
     * ArithmeticException: el panel de resultados respondería 500 justo en el
     * momento en que el administrador lo abre por primera vez.
     */
    private double porcentaje(long parte, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((parte * 1000.0) / total) / 10.0;
    }
    
    private boolean transicionValida(EstadoEncuesta desde, EstadoEncuesta hacia) {
        if (desde == EstadoEncuesta.CREADA && hacia == EstadoEncuesta.ACTIVA) {
            return true;
        }
        if (desde == EstadoEncuesta.ACTIVA && hacia == EstadoEncuesta.CERRADA) {
            return true;
        }
        return false;
    }

    /**
     * Quita espacios, descarta vacios y elimina duplicados conservando el orden
     * en que llegaron. Dos opciones con el mismo texto dejarian al votante sin
     * forma de saber cual escogio.
     */
    private List<String> limpiarOpciones(List<String> entrada) {
        List<String> limpias = new ArrayList<>();
        Set<String> vistas = new HashSet<>();

        if (entrada == null) {
            return limpias;
        }

        for (String texto : entrada) {
            if (texto == null) {
                continue;
            }
            String limpio = texto.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            if (vistas.add(limpio.toLowerCase())) {
                limpias.add(limpio);
            }
        }
        return limpias;
    }
}
