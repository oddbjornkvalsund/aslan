package no.nixx.aslan.pipeline.model;

import java.util.List;

import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.addElement;
import static no.nixx.aslan.core.utils.ListUtils.allButFirstOf;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;

public class Command {

    private final int identity;
    private final List<Argument> arguments;
    private final CommandProperties properties;

    public Command() {
        this.identity = identityHashCode(this);
        this.arguments = emptyList();
        this.properties = CommandProperties.undefined();
    }

    public Command(Argument... arguments) {
        this.identity = identityHashCode(this);
        this.arguments = unmodifiableList(asList(arguments));
        this.properties = CommandProperties.undefined();
    }

    public Command(CommandProperties properties, Argument... arguments) {
        this.identity = identityHashCode(this);
        this.arguments = unmodifiableList(asList(arguments));
        this.properties = properties;
    }

    public Command(Command parent, List<Argument> arguments) {
        this.identity = parent.identity;
        this.arguments = unmodifiableList(arguments);
        this.properties = parent.properties;
    }

    public Command(Command parent, CommandProperties properties) {
        this.identity = parent.identity;
        this.arguments = parent.arguments;
        if (parent.properties.isUndefined()) {
            this.properties = properties;
        } else {
            throw new IllegalStateException("Could not add properties to Command with already defined properties: " + this);
        }
    }

    public int getIdentity() {
        return identity;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public Argument getArgumentAtPosition(int position) {
        final Literal emptyArgument = new Literal("", new ArgumentProperties(position, position, ""));
        return arguments.stream().filter(a -> a.spansPosition(position)).findFirst().orElse(emptyArgument);
    }

    public boolean isFirstArgument(Argument argument) {
        if(spansPosition(argument.getStartIndex()) && spansPosition(argument.getStopIndex())) {
            if(arguments.isEmpty()) {
                return true;
            } else {
                final Argument firstNonEmptyArgument = arguments.get(0);
                return argument.getStartIndex() <= firstNonEmptyArgument.getStartIndex() && argument.getStopIndex() <= firstNonEmptyArgument.getStopIndex();
            }
        } else {
            return false;
        }
    }

    // NOTE: This method does not return the executable name!
    public List<String> getRenderedArguments() {
        return allButFirstOf(arguments).stream().map(Argument::getRenderedText).collect(toList());
    }

    public List<Argument> getPrecedingArguments(Argument arg) {
        return arguments.stream().filter(a -> a.precedes(arg)).collect(toList());
    }

    public Command addArgument(Argument argumentToAdd) {
        return new Command(this, addElement(arguments, argumentToAdd));
    }

    public Command addProperties(CommandProperties properties) {
        return new Command(this, properties);
    }

    public int getStartIndex() {
        return properties.startIndex;
    }

    public int getStopIndex() {
        return properties.stopIndex;
    }

    public String getExecutableName() {
        final Argument firstArgument = firstOf(this.arguments);
        if (firstArgument.isRenderable()) {
            return firstArgument.getRenderedText();
        } else {
            throw new IllegalStateException("Executable name not renderable without command execution: " + firstArgument);
        }
    }

    public boolean spansPosition(int position) {
        return properties.startIndex <= position && properties.stopIndex >= position;
    }

    @Override
    public String toString() {
        return "Command{" +
                "identity=" + identity +
                ", arguments=" + arguments +
                ", properties=" + properties +
                '}';
    }
}