package no.nixx.wing.pipeline;

public class CommandSubstitution extends Argument {

    public final Pipeline pipeline;

    public CommandSubstitution(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public boolean isCommandSubstitution() {
        return true;
    }
}
