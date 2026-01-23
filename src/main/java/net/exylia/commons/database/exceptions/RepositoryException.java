package net.exylia.commons.database.exceptions;

@Deprecated
public class RepositoryException extends DatabaseException {

    private final String repositoryMethod;

    public RepositoryException(String repositoryMethod, String entityClass, String message, Throwable cause) {
        super("Repository Operation", entityClass, "Repository", message, cause);
        this.repositoryMethod = repositoryMethod;
    }

    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Repository Operation Failed:\n");
        sb.append("  Method: ").append(repositoryMethod != null ? repositoryMethod : "Unknown").append("\n");
        sb.append("  Entity: ").append(getEntityClass() != null ? getEntityClass() : "Unknown").append("\n");
        sb.append("  Message: ").append(getMessage()).append("\n");

        if (getCause() != null) {
            sb.append("  Root Cause: ").append(getCause().getClass().getSimpleName())
                    .append(" - ").append(getCause().getMessage()).append("\n");

            sb.append("  Stack Trace:\n");
            StackTraceElement[] stack = getCause().getStackTrace();
            for (int i = 0; i < Math.min(5, stack.length); i++) {
                sb.append("    at ").append(stack[i].toString()).append("\n");
            }
        }

        return sb.toString();
    }

    public String getRepositoryMethod() { return repositoryMethod; }
}
