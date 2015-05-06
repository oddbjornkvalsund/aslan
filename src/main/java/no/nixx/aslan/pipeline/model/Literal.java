package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class Literal extends Argument {

    public final String text;

    public Literal(String text, int startIndex, int stopIndex, String unprocessedArgument) {
        super(startIndex, stopIndex, unprocessedArgument);
        this.text = checkNotNull(text);
    }

    @Override
    public boolean isRenderable() {
        return true;
    }

    @Override
    public String getRenderedText() {
        return text;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Literal) {
            final Literal that = (Literal) obj;
            return this.text.equals(that.text);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }
}
