package net.exylia.commons.redis.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import static net.exylia.commons.utils.DebugUtils.logInternalError;

public class GsonRedisSerializer implements RedisSerializer {

    private final Gson gson;

    public GsonRedisSerializer() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    public GsonRedisSerializer(Gson customGson) {
        this.gson = customGson != null ? customGson : new Gson();
    }

    @Override
    public <T> String serialize(T object) {
        if (object == null) {
            return null;
        }

        try {
             
            if (object instanceof String) {
                return (String) object;
            }

            if (isPrimitiveOrWrapper(object.getClass())) {
                return object.toString();
            }

            return gson.toJson(object);

        } catch (Exception e) {
            logInternalError("Error serializando objeto: " + e.getMessage());
            return null;
        }
    }

    @Override
    public <T> T deserialize(String data, Class<T> type) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        try {
             
            if (type == String.class) {
                return type.cast(data);
            }

            if (isPrimitiveOrWrapper(type)) {
                return deserializePrimitive(data, type);
            }

            return gson.fromJson(data, type);

        } catch (JsonSyntaxException e) {
            logInternalError("Error deserializando JSON: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logInternalError("Error deserializando objeto: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean canSerialize(Class<?> type) {
         
        return type != null && !type.isArray() && !type.isInterface();
    }

    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Boolean.class || type == Character.class ||
                type == Byte.class || type == Short.class ||
                type == Integer.class || type == Long.class ||
                type == Float.class || type == Double.class;
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializePrimitive(String data, Class<T> type) {
        try {
            if (type == boolean.class || type == Boolean.class) {
                return (T) Boolean.valueOf(data);
            } else if (type == char.class || type == Character.class) {
                return (T) Character.valueOf(data.length() > 0 ? data.charAt(0) : '\0');
            } else if (type == byte.class || type == Byte.class) {
                return (T) Byte.valueOf(data);
            } else if (type == short.class || type == Short.class) {
                return (T) Short.valueOf(data);
            } else if (type == int.class || type == Integer.class) {
                return (T) Integer.valueOf(data);
            } else if (type == long.class || type == Long.class) {
                return (T) Long.valueOf(data);
            } else if (type == float.class || type == Float.class) {
                return (T) Float.valueOf(data);
            } else if (type == double.class || type == Double.class) {
                return (T) Double.valueOf(data);
            }
        } catch (NumberFormatException e) {
            logInternalError("Error convirtiendo '" + data + "' a " + type.getSimpleName());
        }

        return null;
    }

    public Gson getGson() {
        return gson;
    }
}
