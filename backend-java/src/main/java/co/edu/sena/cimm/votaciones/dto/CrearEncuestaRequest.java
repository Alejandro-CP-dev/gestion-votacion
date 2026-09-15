package co.edu.sena.cimm.votaciones.dto;

import java.util.List;

/**
 * Cuerpo de POST /api/encuestas
 *
 * {
 *   "titulo": "Eleccion de Representante 2026",
 *   "descripcion": "Vota por tu representante ante el Consejo Academico",
 *   "creadoPor": 1,
 *   "opciones": ["Laura Aprendiz", "Andres Aprendiz", "Voto en blanco"]
 * }
 *
 * Las opciones llegan como texto plano y no como objetos: en la creacion no
 * tienen id (no existen todavia) ni conteo (arranca en cero) ni orden (lo da
 * la posicion en la lista). Pedir un objeto completo obligaria al frontend a
 * mandar campos vacios que el backend ignoraria.
 */
public class CrearEncuestaRequest {
 
    public String titulo;
    public String descripcion;
    public int creadoPor;
    public List<String> opciones;
}
