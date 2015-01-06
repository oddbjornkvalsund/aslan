package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.notNull;

public class CommandSubstitution extends Argument {

    private final Pipeline pipeline;

    public CommandSubstitution(Pipeline pipeline) {
        this.pipeline = notNull(pipeline);
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public boolean isCommandSubstitution() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CommandSubstitution) {
            final CommandSubstitution that = (CommandSubstitution) obj;
            return this.pipeline.equals(that.pipeline);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return pipeline.hashCode();
    }
}