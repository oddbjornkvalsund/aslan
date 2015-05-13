package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static no.nixx.aslan.core.utils.ListUtils.addElement;
import static no.nixx.aslan.core.utils.ListUtils.allButFirstOf;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;

public class Command {

    public final int identity;
    private final List<Argument> arguments;

    public Command() {
        identity = System.identityHashCode(this);
        arguments = emptyList();
    }

    public Command(Command parent, List<Argument> arguments) {
        this.identity = parent.identity;
        this.arguments = unmodifiableList(arguments);
    }

    public Command addArgument(Argument argumentToAdd) {
        return new Command(this, addElement(arguments, argumentToAdd));
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

    public List<Argument> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "Command{" +
                "arguments=" + arguments +
                '}';
    }
}