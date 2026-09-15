package co.edu.sena.cimm.votaciones.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Generacion y hash de los tokens OTP.
 *
 * ================= SecureRandom, NO Math.random() =================
 *
 * Math.random() y new Random() usan un generador congruencial lineal de 48
 * bits sembrado con la hora del sistema. Con unos pocos valores observados se
 * reconstruye la semilla y desde ahi se predicen TODOS los demas, hacia
 * adelante y hacia atras.
 *
 * Traducido a este proyecto: un estudiante recibe su token, y con el calcula
 * los de sus companeros. Puede votar por toda la ficha. El sistema seguiria
 * cumpliendo las cuatro reglas del taller (un voto por token, quema atomica,
 * sin condiciones de carrera, anonimato) y aun asi la eleccion seria falsa.
 *
 * SecureRandom toma entropia del sistema operativo y no es predecible. Es una
 * sola palabra de diferencia en el codigo y es la diferencia entre una
 * votacion y un teatro.
 *
 * ================= SHA-256, NO BCrypt =================
 *
 * Para las claves de usuario se usa BCrypt (AuthService), aqui SHA-256. No es
 * una incoherencia, son dos problemas distintos:
 *
 *  - Una clave la elige una persona, suele ser corta y predecible. BCrypt es
 *    lento a proposito para que probarlas una por una no sea viable.
 *
 *  - Un token lo genera SecureRandom con ~50 bits de entropia. No hay
 *    diccionario que lo adivine, asi que la lentitud no aporta nada.
 *
 * Y hay una razon tecnica que decide el asunto: BCrypt genera una sal distinta
 * por registro, asi que el mismo token produce hashes diferentes cada vez. Con
 * BCrypt seria IMPOSIBLE hacer "WHERE TokenHash = ?"; habria que recorrer la
 * tabla entera comparando uno por uno, unos 100 ms por fila. SHA-256 es
 * determinista, y por eso el TokenHash puede llevar un indice UNIQUE y
 * bloquearse con SELECT ... FOR UPDATE en el paso 6.
 */
public final class TokenUtil {
    
    /**
     * Sin I, L, O, 0 ni 1: el estudiante transcribe este codigo a mano desde
     * una hoja. Cada caracter ambiguo es un voto que se pierde en soporte.
     * Quedan 31 simbolos; con 10 posiciones son unas 49 bits de entropia.
     */
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD = 10;
 
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public TokenUtil() {
    }
    
    /**
     * Genera un token nuevo, ya formateado para mostrarlo
     * el guion es solo visual; normalizar() lo quita antes de hashear
     */
    
    public static String generarToken(){
        StringBuilder sb = new StringBuilder(LONGITUD + 1);
        
        for (int i = 0; i < LONGITUD; i++) {
            if (i == LONGITUD / 2) {
                sb.append('-');
            }
            // nextInt(n) reparte de forma uniforme entre 0 y n-1. Usar
            // "aleatorio.nextInt() % 31" introduciria sesgo hacia los
            // primeros simbolos del alfabeto.
            sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
    
    /**
     * Deja el token en su forma canonica antes de hashear.
     *
     * Se aplica en los DOS extremos: al generarlo y al recibirlo en el voto.
     * Asi da igual si el votante escribe "abcde-fghjk", "ABCDEFGHJK" o
     * " abcde fghjk ": el hash es el mismo. Sin esto, la mitad de los votantes
     * veria "token invalido" por haber escrito en minusculas.
     */
    public static String normalizar(String token) {
        if (token == null) {
            return "";
        }
        return token.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
    
    /** SHA-256 del token normalizado, en hexadecimal de 64 caracteres. */
    public static String hashDeToken(String token) {
        return sha256(normalizar(token));
    }
 
    /** SHA-256 de cualquier texto, en hexadecimal. Lo usa el comprobante. */
    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] resumen = md.digest(texto.getBytes(StandardCharsets.UTF_8));
 
            StringBuilder hex = new StringBuilder(resumen.length * 2);
            for (byte b : resumen) {
                // 0xff evita que Java interprete el byte como negativo, y el
                // "0" de relleno mantiene fijos los 64 caracteres.
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) {
                    hex.append('0');
                }
                hex.append(s);
            }
            return hex.toString();
 
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM. Si falta, el entorno esta roto.
            throw new IllegalStateException("La JVM no tiene SHA-256 disponible", e);
        }
    }
}
