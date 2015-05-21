package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.allButFirstOf;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;
import static no.nixx.aslan.core.utils.ListUtils.lastOf;

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
        if (firstArgument.isLiteral()) {
            return ((Literal) firstArgument).text;
        } else {
            throw new IllegalArgumentException("Executable name not a literal: " + firstArgument);
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
