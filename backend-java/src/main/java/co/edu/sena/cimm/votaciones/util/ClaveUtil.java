package co.edu.sena.cimm.votaciones.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Todo lo que toca hashes de clave, en un solo lugar. Antes vivia duplicado
 * (o a punto de duplicarse) entre AuthService y UsuarioService; la
 * normalizacion del prefijo $2y$/$2a$ es el tipo de detalle que solo hay que
 * acertar una vez.
 */
public final class ClaveUtil {

    private ClaveUtil() {
    }

    public static String hash(String claveEnTextoPlano) {
        return BCrypt.hashpw(claveEnTextoPlano, BCrypt.gensalt());
    }

    /**
     * Verifica la clave contra el hash BCrypt guardado.
     *
     * BCrypt vuelve a hashear la clave usando la sal que viene incrustada en el
     * propio hash y compara. Por eso nunca se hashea la clave por separado para
     * luego compararla con "equals": cada hash de BCrypt lleva su propia sal y
     * dos hashes de la misma clave son distintos entre si.
     *
     * Lo del prefijo: PHP con password_hash() genera hashes "$2y$...", Java con
     * jBCrypt genera "$2a$...". Es el mismo algoritmo; el prefijo 2y solo marca
     * una correccion de 2011 en la implementacion en C. Normalizarlo evita
     * perder una tarde si algun usuario queda creado desde PHP.
     */
    public static boolean coincide(String claveEnTextoPlano, String hashGuardado) {
        if (hashGuardado == null || hashGuardado.length() < 4) {
            return false;
        }
        String hash = hashGuardado.startsWith("$2y$")
                ? "$2a$" + hashGuardado.substring(4)
                : hashGuardado;

        return BCrypt.checkpw(claveEnTextoPlano, hash);
    }
}
