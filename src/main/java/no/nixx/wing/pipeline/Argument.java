package no.nixx.wing.pipeline;

public abstract class Argument {

    public boolean isLiteral() {
        return false;
    }

    public boolean isCommandSubstitution() {
        return false;
    }

    public boolean isVariableSubstitution() {
        return false;
    }
}
