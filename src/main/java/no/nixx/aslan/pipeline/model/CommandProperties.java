package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkArgument;

public class CommandProperties {

    public final int startIndex;
    public final int stopIndex;

    private CommandProperties(int startIndex, int stopIndex, boolean validate) {
        if (validate) {
            checkArgument(startIndex >= 0);
            checkArgument(stopIndex >= 0);
        }
        this.startIndex = startIndex;
        this.stopIndex = stopIndex;
    }

    public CommandProperties(int startIndex, int stopIndex) {
        this(startIndex, stopIndex, true);
    }

    public static CommandProperties undefined() {
        return new CommandProperties(-1, -1, false);
    }

    public boolean isUndefined() {
        return startIndex == -1 && stopIndex == -1;
    }
}