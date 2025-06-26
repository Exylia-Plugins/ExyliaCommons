package net.exylia.commons.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Gestor de placeholders personalizados para menús
 */
@Deprecated
public class CustomPlaceholderManager {

    private static final Map<String, Function<Object, String>> placeholders = new HashMap<>();

    /**
     * Registra un placeholder personalizado
     * @param placeholder El placeholder sin los % (ej: "target")
     * @param replacer Función que recibe un objeto context y devuelve el valor de reemplazo
     */
    public static void register(String placeholder, Function<Object, String> replacer) {
        placeholders.put(placeholder, replacer);
    }

    /**
     * Procesa los placeholders personalizados en un texto
     * @param text Texto con placeholders
     * @param context Objeto de contexto para procesar los placeholders (ej: Player target)
     * @return Texto con placeholders reemplazados
     */
    public static String process(String text, Object context) {
        if (text == null || text.isEmpty()) return text;

        String result = text;
        for (Map.Entry<String, Function<Object, String>> entry : placeholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                String replacement = entry.getValue().apply(context);
                result = result.replace(placeholder, replacement != null ? replacement : "");
            }
        }

        return result;
    }

    /**
     * Limpia todos los placeholders registrados
     */
    public static void clear() {
        placeholders.clear();
    }
}