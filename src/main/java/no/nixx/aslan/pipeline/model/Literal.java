package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class Literal extends Argument {

    public final String text;

    public Literal(String text, ArgumentProperties properties) {
        super(properties);
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
    public String toString() {
        return "Literal{" +
                "text='" + text + '\'' +
                '}';
    }
}