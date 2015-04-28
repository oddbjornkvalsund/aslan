package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class VariableSubstitution extends Argument {
    public final String variableName;

    public VariableSubstitution(String variableName) {
        this.variableName = checkNotNull(variableName);
    }

    @Override
    public boolean isRenderableTextAvailableWithoutCommmandExecution() {
        return false;
    }

    @Override
    public String getRenderableText() {
        throw new IllegalStateException("Renderable text is not available without commmand execution: " + this);
    }

    @Override
    public boolean isVariableSubstitution() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VariableSubstitution) {
            final VariableSubstitution that = (VariableSubstitution) obj;
            return this.variableName.equals(that.variableName);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return variableName.hashCode();
    }
}
