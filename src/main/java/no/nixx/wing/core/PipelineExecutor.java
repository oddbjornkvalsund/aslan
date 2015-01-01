package no.nixx.wing.core;

import no.nixx.wing.pipeline.model.Pipeline;

public interface PipelineExecutor {
    void execute(ExecutionContext context, Pipeline pipeline);
}
