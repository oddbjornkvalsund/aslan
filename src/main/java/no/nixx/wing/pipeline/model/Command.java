package no.nixx.wing.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class Command {
    private final List<Argument> arguments = new ArrayList<Argument>();

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
        if (this.arguments.get(0).isLiteral()) {
            return ((Literal) this.arguments.get(0)).text;
        } else {
            throw new IllegalArgumentException("Executable name not a literal: " + this.arguments.get(0));
        }
    }

    public List<String> getArgumentsAsStrings() {
        // TODO: Perhaps a bad idea!
        final List<String> argumentsAsStrings = new ArrayList<String>();

        for (Argument argument : this.arguments.subList(1, this.arguments.size())) {
            if (argument.isLiteral()) {
                argumentsAsStrings.add(((Literal) argument).text);
            } else {
                throw new IllegalArgumentException("Executable name not a literal: " + argument);
            }
        }

        return argumentsAsStrings;
    }

    public List<Argument> getArgumentsUnmodifiable() {
        return unmodifiableList(arguments);
    }

}
