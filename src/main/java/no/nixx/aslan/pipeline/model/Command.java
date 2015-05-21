package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.addElement;
import static no.nixx.aslan.core.utils.ListUtils.allButFirstOf;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;

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

    public List<String> getRenderedArguments() {
        return allButFirstOf(arguments).stream().map(Argument::getRenderableText).collect(toList());
    }

    public List<Argument> getArgumentsUnmodifiable() {
        return unmodifiableList(arguments);
    }

    public boolean spansPosition(int position) {
        if (arguments.isEmpty()) {
            return false;
        } else {
            final Argument first = firstOf(arguments);
            final Argument last = lastOf(arguments);

            return first.startIndex <= position && last.stopIndex > position;
        }
    }

    public Argument getArgumentAtPosition(int position) {
        for (Argument argument : arguments) {
            if (argument.spansPosition(position)) {
                return argument;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "Command{" +
                "arguments=" + arguments +
                '}';
    }
}