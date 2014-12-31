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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableSubstitution that = (VariableSubstitution) o;

        if (variableName != null ? !variableName.equals(that.variableName) : that.variableName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return variableName != null ? variableName.hashCode() : 0;
    }
}
