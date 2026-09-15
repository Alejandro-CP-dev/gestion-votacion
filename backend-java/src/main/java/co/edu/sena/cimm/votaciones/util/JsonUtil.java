package co.edu.sena.cimm.votaciones.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author Usuario
 */
public final class JsonUtil {
    
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
 
    /** Constructor privado: es una clase de utilidades, no se instancia. */
    private JsonUtil() {
    }
 
    public static String aJson(Object objeto) {
        return GSON.toJson(objeto);
    }
 
    public static <T> T desdeJson(String json, Class<T> clase) {
        return GSON.fromJson(json, clase);
    }
 
    /**
     * Gson no sabe manejar LocalDateTime por si solo (es de java.time y Gson
     * es anterior). Este adaptador le ensena a leerlo y escribirlo en ISO:
     * "2026-09-15T14:30:00".
     */
    static class LocalDateTimeAdapter
            implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
 
        private static final DateTimeFormatter FORMATO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
 
        @Override
        public JsonElement serialize(LocalDateTime valor, Type tipo, JsonSerializationContext ctx) {
            if (valor == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(valor.format(FORMATO));
        }
 
        @Override
        public LocalDateTime deserialize(JsonElement json, Type tipo, JsonDeserializationContext ctx) {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            String texto = json.getAsString();
 
            // Los <input type="datetime-local"> del HTML envian "2026-09-15T14:30"
            // sin los segundos. Se los agregamos para que el formato ISO calce.
            if (texto.length() == 16) {
                texto = texto + ":00";
            }
            return LocalDateTime.parse(texto, FORMATO);
        }
    }
}
