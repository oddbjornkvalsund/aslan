package no.nixx.wing.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class Pipeline {
    private final List<Command> commands = new ArrayList<Command>();

    public void addCommand(Command command) {
        this.commands.add(command);
    }

    public List<Command> getCommandsUnmodifiable() {
        return unmodifiableList(commands);
    }

}