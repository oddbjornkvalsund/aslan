package no.nixx.aslan.pipeline.model;

import no.nixx.aslan.core.utils.ListUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

public class Pipeline {

    private final List<Command> commands;

    public Pipeline() {
        this.commands = emptyList();
    }

    public Pipeline(List<Command> commands) {
        this.commands = unmodifiableList(commands);
    }

    public Pipeline addCommand(Command command) {
        return new Pipeline(ListUtils.addElement(commands, command));
    }

    public List<Command> getCommandsUnmodifiable() {
        return unmodifiableList(commands);
    }

    public Command getCommandAtPosition(int position) {
        for (Command command : commands) {
            if (command.spansPosition(position)) {
                final Argument argument = command.getArgumentAtPosition(position);
                if (argument != null && argument.isCommandSubstitution()) {
                    final CommandSubstitution cs = (CommandSubstitution) argument;
                    return cs.getPipeline().getCommandAtPosition(position);
                } else {
                    return command;
                }
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "Pipeline{" +
                "commands=" + commands +
                '}';
    }
}