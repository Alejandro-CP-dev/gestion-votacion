/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.repository;

import co.edu.sena.cimm.votaciones.model.Encuesta;
import co.edu.sena.cimm.votaciones.model.EstadoEncuesta;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Usuario
 */
public interface EncuestaRepository {

    /**
     * Inserta la encuesta y todas sus opciones en UNA transaccion.
     *
     * Si falla la insercion de la tercera opcion, no puede quedar una encuesta
     * con dos. Una votacion a la que le falta un candidato es peor que una
     * votacion que no se creo.
     *
     * Devuelve la encuesta con los id que genero la base de datos.
     */
    Encuesta crear(Encuesta encuesta);

    /**
     * Trae la encuesta con sus opciones ya cargadas y ordenadas por Orden.
     */
    Optional<Encuesta> buscarPorId(int id);
    
    /** Todas las encuestas, sin filtrar. La necesita el panel del administrador. */
    List<Encuesta> listarTodas();

    List<Encuesta> listarPorEstado(EstadoEncuesta estado);

    /**
     * Cambia el estado solo si la encuesta sigue en el estado esperado.
     *
     * CAMBIO RESPECTO AL PASO 2: la firma recibe ahora el estado actual. La
     * version anterior, cambiarEstado(id, nuevoEstado), obligaba al servicio a
     * leer el estado y despues escribirlo en dos operaciones separadas, que es
     * exactamente la condicion de carrera que el taller pide evitar. Con la
     * guarda, si otro administrador cerro la encuesta en el intervalo, el
     * UPDATE afecta 0 filas y esto devuelve false.
     *
     * Quien decide QUE transiciones son validas sigue siendo el servicio. El
     * repositorio solo garantiza que el cambio sea atomico.
     *
     * @return true si el cambio se aplico, false si la encuesta ya no estaba en
     * el estado esperado.
     */
    boolean cambiarEstado(int encuestaId, EstadoEncuesta estadoActual, EstadoEncuesta nuevoEstado);

    /**
     * Verifica que una opcion pertenezca a una encuesta.
     *
     * Sin esto, alguien podria mandar {"token":"el mio","opcionId":99} y sumar
     * un voto a una encuesta ajena con un token valido de otra. El id que llega
     * por HTTP nunca se usa sin comprobar a que pertenece.
     */
    boolean opcionPerteneceAEncuesta(int opcionId, int encuestaId);
}
