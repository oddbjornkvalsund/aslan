package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class CommandSubstitution extends Argument {

    private final Pipeline pipeline;

    public CommandSubstitution(Pipeline pipeline, int startIndex, int stopIndex, String unprocessedArgument) {
        super(startIndex, stopIndex, unprocessedArgument);
        this.pipeline = checkNotNull(pipeline);
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public boolean isRenderable() {
        return false;
    }

    @Override
    public String getRenderedText() {
        throw new IllegalStateException("Renderable text is not available without commmand execution: " + this);
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