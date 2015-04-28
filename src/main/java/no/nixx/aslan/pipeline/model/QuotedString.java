package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

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
        if (argument.isCommandSubstitution() || argument.isVariableSubstitution()) {
            this.components.add(new Component(this.text.length(), argument));
        } else {
            throw new IllegalArgumentException("Invalid argument type: " + argument);
        }
    }

    @Override
    public boolean isRenderableTextAvailableWithoutCommmandExecution() {
        return components.stream().allMatch(c -> c.argument.isRenderableTextAvailableWithoutCommmandExecution());
    }

    @Override
    public String getRenderableText() {
        if (!isRenderableTextAvailableWithoutCommmandExecution()) {
            throw new IllegalStateException("Renderable text is not available without commmand execution: " + this);
        }

        final StringBuilder sb = new StringBuilder(getText());

        int offset = 0;
        for (Component component : components) {
            if (component.argument.isRenderableTextAvailableWithoutCommmandExecution()) {
                final String renderableText = component.argument.getRenderableText();
                sb.insert(component.position + offset, renderableText);
                offset += renderableText.length();
            }
        }

        return sb.toString();
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