package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public abstract class Argument {

    private final ArgumentProperties properties;

    // This constructor should only be used by special subclasses such as ExpandedArgument in PipelineExecutor
    public Argument() {
        this.properties = ArgumentProperties.undefined();
    }

    public Argument(ArgumentProperties properties) {
        this.properties = checkNotNull(properties);
    }

    public abstract boolean isRenderable();

    public abstract String getRenderedText();

    public ArgumentProperties getProperties() {
        return properties;
    }

    public boolean spansPosition(int position) {
        return properties.startIndex <= position && properties.stopIndex > position;
    }

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
        return properties.startIndex;
    }

    public int getStopIndex() {
        return properties.stopIndex;
    }

    public String getUnprocessedArgument() {
        return properties.unprocessedArgument;
    }
}
