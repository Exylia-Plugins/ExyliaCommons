package net.exylia.commons.database.exceptions;

@Deprecated
public class SerializationException extends DatabaseException {

    private final String fieldName;
    private final String serializationType;
    private final Object originalValue;

    public SerializationException(String operation, String entityClass, String fieldName, String serializationType, Object originalValue, String message, Throwable cause) {
        super(operation, entityClass, "Serialization", message, cause);
        this.fieldName = fieldName;
        this.serializationType = serializationType;
        this.originalValue = originalValue;
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Serialization Error:\n");
        sb.append("  Operation: ").append(getOperation()).append("\n");
        sb.append("  Entity: ").append(getEntityClass()).append("\n");
        sb.append("  Field: ").append(fieldName).append("\n");
        sb.append("  Serialization Type: ").append(serializationType).append("\n");
        sb.append("  Original Value Type: ").append(originalValue != null ? originalValue.getClass().getSimpleName() : "null").append("\n");
        sb.append("  Original Value: ").append(originalValue != null ? originalValue.toString() : "null").append("\n");
        sb.append("  Message: ").append(getMessage()).append("\n");

        if (getCause() != null) {
            sb.append("  Root Cause: ").append(getCause().getClass().getSimpleName())
                    .append(" - ").append(getCause().getMessage()).append("\n");
        }

        return sb.toString();
    }

    public String getFieldName() { return fieldName; }
    public String getSerializationType() { return serializationType; }
    public Object getOriginalValue() { return originalValue; }
}
