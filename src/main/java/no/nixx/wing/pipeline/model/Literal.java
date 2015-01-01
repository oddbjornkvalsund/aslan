package no.nixx.wing.pipeline.model;

public class Literal extends Argument {

    public final String text;

    public Literal(String text) {
        this.text = text;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Literal literal = (Literal) o;

        if (text != null ? !text.equals(literal.text) : literal.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return text != null ? text.hashCode() : 0;
    }
}
