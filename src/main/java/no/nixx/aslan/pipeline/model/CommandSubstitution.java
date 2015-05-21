package no.nixx.aslan.pipeline.model;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class CommandSubstitution extends Argument {

    private final Pipeline pipeline;

    public CommandSubstitution(Pipeline pipeline, ArgumentProperties properties) {
        super(properties);
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
        throw new UnsupportedOperationException("Renderable text is not available without commmand execution: " + this);
    }

    @Override
    public boolean isCommandSubstitution() {
        return true;
    }

    @Override
    public String toString() {
        return "CommandSubstitution{" +
                "pipeline=" + pipeline +
                '}';
    }
}