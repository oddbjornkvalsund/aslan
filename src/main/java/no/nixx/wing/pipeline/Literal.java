package no.nixx.wing.pipeline;

public class Literal extends Argument {

    public final String text;

    public Literal(String text) {
        this.text = text;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }
}
