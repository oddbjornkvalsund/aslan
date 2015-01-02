package no.nixx.wing.pipeline.model;

import static no.nixx.wing.core.utils.Preconditions.notNull;

public class VariableSubstitution extends Argument {
    public final String variableName;

    public VariableSubstitution(String variableName) {
        this.variableName = notNull(variableName);
    }

    @Override
    public boolean isVariableSubstitution() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof VariableSubstitution) {
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
