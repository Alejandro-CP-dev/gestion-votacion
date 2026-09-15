package co.edu.sena.cimm.votaciones.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lectura de configuracion con un orden de prioridad fijo:
 *
 *   1. Variable de entorno
 *   2. Archivo db.properties del classpath
 *   3. Valor por defecto
 *
 * POR QUE EXISTE ESTA CLASE AHORA Y NO ANTES:
 * en el paso 1 solo Database leia configuracion, asi que la logica vivia
 * adentro y estaba bien ahi. Ahora ApiKeyFilter necesita lo mismo. La segunda
 * vez que algo se necesita es el momento de extraerlo; la primera, no.
 * Adelantarse produce abstracciones que resuelven problemas que nunca llegan.
 *
 * Consecuencia: Database.java cambia para usar esta clase en vez de su propio
 * metodo privado. Dos formas distintas de leer configuracion en el mismo
 * proyecto es justo la clase de inconsistencia que despues nadie recuerda.
 */
public final class Configuracion {
    
    private static final Properties PROPIEDADES = cargar();
 
    private Configuracion() {
    }
 
    public static String obtener(String clave, String variableEntorno, String porDefecto) {
        String v = System.getenv(variableEntorno);
 
        if (v == null || v.trim().isEmpty()) {
            v = PROPIEDADES.getProperty(clave);
        }
        if (v == null || v.trim().isEmpty()) {
            v = porDefecto;
        }
        return (v == null) ? null : v.trim();
    }
 
    private static Properties cargar() {
        Properties p = new Properties();
        try (InputStream in = Configuracion.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
            } else {
                System.out.println("[SVIS] No se encontro db.properties, "
                        + "se usaran variables de entorno o valores por defecto");
            }
        } catch (IOException e) {
            System.out.println("[SVIS] No se pudo leer db.properties: " + e.getMessage());
        }
        return p;
    }
}
