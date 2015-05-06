package no.nixx.aslan.pipeline.model;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class QuotedString extends Argument {

    private final String text;
    private final List<Component> components;

    public QuotedString(String text, List<Component> components, int startIndex, int stopIndex, String unprocessedArgument) {
        super(startIndex, stopIndex, unprocessedArgument);
        this.text = checkNotNull(text);
        this.components = unmodifiableList(checkNotNull(components));
    }

    @Override
    public boolean isRenderable() {
        return components.stream().allMatch(c -> c.argument.isRenderable());
    }

    @Override
    public String getRenderedText() {
        final StringBuilder sb = new StringBuilder(getText());

        int offset = 0;
        for (Component component : components) {
            final String text = component.argument.getRenderedText();
            sb.insert(component.position + offset, text);
            offset += text.length();
        }

        return sb.toString();
    }

    public String getText() {
        return text;
    }

    public List<Component> getComponents() {
        return components;
    }

    @Override
    public boolean isQuotedString() {
        return true;
    }

    @Override
    public String toString() {
        return "QuotedString{" +
                "text=" + text +
                ", components=" + components +
                '}';
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
            this.argument = checkNotNull(argument);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Component) {
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