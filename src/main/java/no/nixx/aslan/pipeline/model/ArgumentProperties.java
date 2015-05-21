package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkArgument;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class ArgumentProperties {

    public final int startIndex;
    public final int stopIndex;
    public final String unprocessedArgument;

    private ArgumentProperties(int startIndex, int stopIndex, String unprocessedArgument, boolean validate) {
        if (validate) {
            checkArgument(startIndex >= 0);
            checkArgument(stopIndex >= 0);
            checkNotNull(unprocessedArgument);
        }
        this.startIndex = startIndex;
        this.stopIndex = stopIndex;
        this.unprocessedArgument = unprocessedArgument;
    }

    public ArgumentProperties(int startIndex, int stopIndex, String unprocessedArgument) {
        this(startIndex, stopIndex, unprocessedArgument, true);
    }

    public static ArgumentProperties undefined() {
        return new ArgumentProperties(-1, -1, null, false);
    }
}
