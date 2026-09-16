package co.edu.sena.cimm.votaciones.util;

import co.edu.sena.cimm.votaciones.config.Configuracion;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado reversible del token OTP, para poder mostrarselo de nuevo al
 * estudiante desde su panel.
 *
 * POR QUE ESTO Y NO SOLO EL HASH: TokenUtil.hashDeToken() usa SHA-256, que a
 * proposito no se puede revertir (ver el comentario de esa clase). Eso basta
 * para VALIDAR un token contra la base de datos, pero no alcanza para
 * MOSTRARLO: si el administrador genero el padron y el estudiante perdio su
 * volante, con solo el hash no hay forma de recuperar el codigo.
 *
 * AES-256-GCM en vez de guardar el token tal cual: la llave vive en
 * db.properties/SVIS_TOKEN_KEY, fuera de la base de datos, asi que una fuga
 * de la sola base de datos no expone los tokens sin usar. Es una garantia mas
 * debil que la de TokenHash (que no depende de proteger ningun secreto), y
 * hay que poder decirlo asi: es el costo exacto de la funcionalidad que se
 * pidio (que el estudiante pueda ver su token otra vez).
 */
public final class TokenCifradoUtil {

    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int TAMANO_IV = 12;
    private static final int TAMANO_TAG_BITS = 128;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenCifradoUtil() {
    }

    public static String cifrar(String tokenEnClaro) {
        try {
            byte[] iv = new byte[TAMANO_IV];
            ALEATORIO.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, llave(), new GCMParameterSpec(TAMANO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(tokenEnClaro.getBytes(StandardCharsets.UTF_8));

            byte[] salida = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, salida, 0, iv.length);
            System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);

            return Base64.getEncoder().encodeToString(salida);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible cifrar el token", e);
        }
    }

    public static String descifrar(String valorAlmacenado) {
        try {
            byte[] entrada = Base64.getDecoder().decode(valorAlmacenado);
            byte[] iv = Arrays.copyOfRange(entrada, 0, TAMANO_IV);
            byte[] cifrado = Arrays.copyOfRange(entrada, TAMANO_IV, entrada.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, llave(), new GCMParameterSpec(TAMANO_TAG_BITS, iv));

            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible descifrar el token", e);
        }
    }

    /**
     * Se relee en cada llamado, no se cachea en un campo static: es la misma
     * cantidad de trabajo (Configuracion ya cachea las Properties) y evita
     * que la clase falle al cargar si alguna vez se usa antes de tener la
     * variable de entorno lista.
     */
    private static SecretKeySpec llave() {
        String base64 = Configuracion.obtener("token.key", "SVIS_TOKEN_KEY", null);
        if (base64 == null || base64.isEmpty()) {
            throw new IllegalStateException(
                    "token.key / SVIS_TOKEN_KEY no esta configurada: no se puede cifrar ni mostrar tokens");
        }

        byte[] bytes = Base64.getDecoder().decode(base64);
        if (bytes.length != 32) {
            throw new IllegalStateException("token.key debe decodificar a 32 bytes (AES-256)");
        }

        return new SecretKeySpec(bytes, "AES");
    }
}
