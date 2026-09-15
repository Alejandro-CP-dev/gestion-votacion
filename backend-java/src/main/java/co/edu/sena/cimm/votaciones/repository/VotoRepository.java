/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package co.edu.sena.cimm.votaciones.repository;

/**
 * El repositorio que no corresponde a ninguna tabla.
 *
 * No existe la tabla Voto (esa fue la decision que cumple la REGLA 4), y sin
 * embargo emitir un voto necesita su propio repositorio, porque toca TokenOtp,
 * Opcion y Comprobante en una sola transaccion indivisible.
 *
 * Esa es la idea que hay que poder defender: un repositorio representa una
 * unidad de trabajo transaccional, no una tabla. Si el limite fuera la tabla,
 * esta transaccion tendria que orquestarse desde el servicio, pasando una
 * Connection de un repositorio a otro, y el servicio terminaria sabiendo de
 * JDBC. En este diseno el servicio no sabe que existe una base de datos.
 *
 * Una sola operacion publica, y es la razon de ser de todo el taller.
 */
public interface VotoRepository {
    /**
     * Emite un voto de forma atomica. En una sola transaccion:
     *
     *   1. Bloquea la fila del token (SELECT ... FOR UPDATE)
     *   2. Verifica que este DISPONIBLE, no expirado y que la encuesta este ACTIVA
     *   3. Lo marca USADO                      <- REGLA 2
     *   4. Suma +1 al contador de la opcion    <- REGLA 4
     *   5. Inserta el comprobante anonimo
     *
     * Fija el limite del bloqueo por una razon deliberada: recibe el hash del
     * token, no el id del usuario. Ni siquiera el codigo que ejecuta el voto
     * sabe de quien es. No se puede filtrar lo que no se recibe.
     *
     * @param tokenHash SHA-256 en hexadecimal del token que digito el votante
     * @param opcionId  la opcion elegida
     * @return el codigo de recibo anonimo (SHA-256) que se le muestra al votante
     *
     * @throws ConflictoException si el token ya estaba USADO. El servlet lo
     *         traduce a HTTP 409 y eso es exactamente lo que el instructor va a
     *         medir en la prueba de doble voto en paralelo.
     * @throws NegocioException si el token no existe, expiro, la encuesta no
     *         esta ACTIVA o la opcion no pertenece a esa encuesta.
     */
    String emitir(String tokenHash, int opcionId);
}
