package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static no.nixx.aslan.core.utils.ListUtils.allButFirstOf;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;

public class Command {

    private final List<Argument> arguments = new ArrayList<>();

    public void addArgument(Argument argument) {
        this.arguments.add(argument);
    }

    public void replaceArgument(Argument oldArgument, Argument newArgument) {
        if (this.arguments.contains(oldArgument)) {
            this.arguments.set(this.arguments.indexOf(oldArgument), newArgument);
        } else {
            throw new IllegalArgumentException("No such element: " + oldArgument);
        }
    }

    public String getExecutableName() {
        final Argument firstArgument = firstOf(this.arguments);
        if (firstArgument.isRenderable()) {
            return firstArgument.getRenderedText();
        } else {
            throw new IllegalStateException("Executable name not renderable without command execution: " + firstArgument);
        }
    }

    public List<String> getArgumentsAsStrings() {
        // TODO: Perhaps a bad idea!
        final List<String> argumentsAsStrings = new ArrayList<>();
        for (Argument argument : allButFirstOf(this.arguments)) {
            if (argument.isRenderable()) {
                argumentsAsStrings.add(argument.getRenderedText());
            } else {
                throw new IllegalStateException("Argument not renderable without command execution: " + argument);
            }
        }
        return argumentsAsStrings;
    }

    public List<Argument> getArgumentsUnmodifiable() {
        return unmodifiableList(arguments);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Command) {
            final Command that = (Command) obj;
            return this.arguments.equals(that.arguments);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return arguments.hashCode();
    }
}