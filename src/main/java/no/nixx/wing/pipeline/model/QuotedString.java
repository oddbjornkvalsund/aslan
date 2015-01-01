package no.nixx.wing.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class QuotedString extends Argument {

    private final StringBuilder text;
    private final List<QuotedStringComponent> components;

    public QuotedString() {
        this.text = new StringBuilder();
        this.components = new ArrayList<>();
    }

    public void setText(String text) {
        this.text.replace(0, this.text.length(), text);
    }
    public void appendText(String text) {
        this.text.append(text);
    }

    public void addComponent(Argument argument) {
        this.components.add(new QuotedStringComponent(this.text.length(), argument));
    }

    public void addComponent(int position, Argument argument) {
        this.components.add(new QuotedStringComponent(position, argument));
    }

    public String getText() {
        return text.toString();
    }

    public List<QuotedStringComponent> getComponentsUnmodifiable() {
        return unmodifiableList(this.components);
    }

    @Override
    public boolean isQuotedString() {
        return true;
    }

    public static class QuotedStringComponent {

        public final int position;
        public final Argument argument;

        public QuotedStringComponent(int position, Argument argument) {
            this.position = position;
            this.argument = argument;
        }
    }
}