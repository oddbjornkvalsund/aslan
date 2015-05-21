package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class VariableSubstitution extends Argument {
    public final String variableName;

    public VariableSubstitution(String variableName, int startIndex, int stopIndex, String unprocessedArgument) {
        super(startIndex, stopIndex, unprocessedArgument);
        this.variableName = checkNotNull(variableName);
    }

    @Override
    public boolean isRenderable() {
        return false;
    }

    @Override
    public String getRenderedText() {
        throw new UnsupportedOperationException("Renderable text is not available without commmand execution: " + this);
    }

    @Override
    public boolean isVariableSubstitution() {
        return true;
    }

    @Override
    public String toString() {
        return "VariableSubstitution{" +
                "variableName='" + variableName + '\'' +
                '}';
    }
}