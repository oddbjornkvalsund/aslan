package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class Pipeline { // TODO: Make immutable

    private final List<Command> commands = new ArrayList<>();

    public Pipeline() {
    }

    public Pipeline(List<Command> commands) {
        this.commands.addAll(commands);
    }

    public void addCommand(Command command) {
        this.commands.add(command);
    }

    public List<Command> getCommandsUnmodifiable() {
        return unmodifiableList(commands);
    }

    @Override
    public String toString() {
        return "Pipeline{" +
                "commands=" + commands +
                '}';
    }
}