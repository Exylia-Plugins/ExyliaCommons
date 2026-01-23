package net.exylia.commons.redis.serialization;

import java.io.*;
import java.util.Base64;

import static net.exylia.commons.utils.DebugUtils.logInternalError;

@Deprecated
public class JavaRedisSerializer implements RedisSerializer {

    @Override
    public <T> String serialize(T object) {
        if (object == null) {
            return null;
        }

        if (!(object instanceof Serializable)) {
            throw new IllegalArgumentException("Objeto debe implementar Serializable");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(object);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);

        } catch (IOException e) {
            logInternalError("Error serializando objeto: " + e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String data, Class<T> type) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(data);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {

                Object obj = ois.readObject();
                return type.cast(obj);

            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            logInternalError("Error deserializando objeto: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean canSerialize(Class<?> type) {
        return Serializable.class.isAssignableFrom(type);
    }
}
