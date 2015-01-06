package no.nixx.aslan.pipeline.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class Pipeline {
    private final List<Command> commands = new ArrayList<>();

    public void addCommand(Command command) {
        this.commands.add(command);
    }

    public List<Command> getCommandsUnmodifiable() {
        return unmodifiableList(commands);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pipeline) {
            final Pipeline that = (Pipeline) obj;
            return this.commands.equals(that.commands);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return commands.hashCode();
    }
}