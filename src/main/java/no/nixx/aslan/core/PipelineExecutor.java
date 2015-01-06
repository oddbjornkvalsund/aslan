package no.nixx.aslan.core;

import no.nixx.aslan.pipeline.model.Pipeline;

public interface PipelineExecutor {
    void execute(ExecutionContext context, Pipeline pipeline);
}
