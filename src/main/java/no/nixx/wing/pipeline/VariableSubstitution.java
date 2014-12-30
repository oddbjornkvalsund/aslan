package no.nixx.wing.pipeline;

public class VariableSubstitution extends Argument {
    public final String variableName;

    public VariableSubstitution(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public boolean isVariableSubstitution() {
        return true;
    }
}
