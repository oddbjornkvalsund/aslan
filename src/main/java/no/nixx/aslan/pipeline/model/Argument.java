package no.nixx.aslan.pipeline.model;

public abstract class Argument {

    public abstract boolean isRenderableTextAvailableWithoutCommmandExecution();

    public abstract String getRenderableText();

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
}
