package no.nixx.wing.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static no.nixx.wing.core.utils.Preconditions.notNull;

public class QuotedString extends Argument {

    private final StringBuilder text;
    private final List<Component> components;

    public QuotedString() {
        this.text = new StringBuilder();
        this.components = new ArrayList<>();
    }

    public void appendText(String text) {
        this.text.append(text);
    }

    public void addComponent(Argument argument) {
        if(argument.isCommandSubstitution() || argument.isVariableSubstitution()) {
            this.components.add(new Component(this.text.length(), argument));
        } else {
            throw new IllegalArgumentException("Invalid argument type: " + argument);
        }
    }

    public String getText() {
        return text.toString();
    }

    public List<Component> getComponentsUnmodifiable() {
        return unmodifiableList(this.components);
    }

    @Override
    public boolean isQuotedString() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QuotedString) {
            final QuotedString that = (QuotedString) obj;
            return this.text.equals(that.text) && this.components.equals(that.components);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return text.hashCode() + components.hashCode();
    }

    public static class Component {

        public final int position;
        public final Argument argument;

        public Component(int position, Argument argument) {
            this.position = position;
            this.argument = notNull(argument);
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Component) {
                final Component that = (Component) obj;
                return this.position == that.position && this.argument.equals(that.argument);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.position + this.argument.hashCode();
        }
    }
}