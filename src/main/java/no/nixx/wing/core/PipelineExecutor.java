package no.nixx.wing.core;

import no.nixx.wing.pipeline.Pipeline;

public interface PipelineExecutor {
    void execute(ExecutionContext context, Pipeline pipeline);
}
