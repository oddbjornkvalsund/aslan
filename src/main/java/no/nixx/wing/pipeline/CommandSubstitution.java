package no.nixx.wing.pipeline;

public class CommandSubstitution extends Argument {

    private final Pipeline pipeline;

    public CommandSubstitution(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public boolean isCommandSubstitution() {
        return true;
    }
}
