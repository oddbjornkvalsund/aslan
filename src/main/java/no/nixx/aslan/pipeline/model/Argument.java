package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkArgument;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public abstract class Argument {

    private final int startIndex;

    private final int stopIndex;

    private final String unprocessedArgument;

    // This constructor should only be used by special subclasses such as ExpandedArgument in PipelineExecutor
    public Argument() {
        this.startIndex = -1;
        this.stopIndex = -1;
        this.unprocessedArgument = null;
    }

    public Argument(int startIndex, int stopIndex, String unprocessedArgument) {
        checkArgument(startIndex >= 0);
        checkArgument(stopIndex >= 0);
        checkNotNull(unprocessedArgument);
        this.startIndex = startIndex;
        this.stopIndex = stopIndex;
        this.unprocessedArgument = unprocessedArgument;
    }

    public abstract boolean isRenderable();

    public abstract String getRenderedText();

    public boolean isLiteral() {
        return false;
    }

    public boolean isQuotedString() {
        return false;
    }

    public boolean isCommandSubstitution() {
        return false;
    }

    public boolean isVariableSubstitution() {
        return false;
    }

    public boolean isCompositeArgument() {
        return false;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public String getUnprocessedArgument() {
        return unprocessedArgument;
    }
}
