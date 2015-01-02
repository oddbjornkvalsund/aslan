package no.nixx.wing.pipeline.model;

import static no.nixx.wing.core.utils.Preconditions.notNull;

public class Literal extends Argument {

    public final String text;

    public Literal(String text) {
        this.text = notNull(text);
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Literal) {
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
