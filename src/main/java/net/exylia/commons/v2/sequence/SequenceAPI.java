package net.exylia.commons.v2.sequence;

import java.util.List;

public final class SequenceAPI {

    private static final SequenceExecutor EXECUTOR = new SequenceExecutor();

    private SequenceAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void execute(SequenceContext ctx, List<String> effects) {
        EXECUTOR.execute(ctx, effects);
    }

    public static SequenceExecutor getExecutor() {
        return EXECUTOR;
    }
}
