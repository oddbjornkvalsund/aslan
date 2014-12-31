package no.nixx.wing.pipeline;

import no.nixx.wing.core.ExecutionContext;
import no.nixx.wing.core.ExecutionContextImpl;
import no.nixx.wing.core.PipelineExecutor;
import no.nixx.wing.core.PipelineExecutorImpl;

public class ParserTest {

    public static void main(String[] args) {
        new ParserTest().run();
    }

    public void run() {
        final ExecutionContext context = new ExecutionContextImpl();
        context.setCurrentWorkingDirectory(System.getProperty("user.dir"));

        final String cmd = "ls | grep git";
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand(cmd);

        final PipelineExecutor pipelineExecutor = new PipelineExecutorImpl();
        pipelineExecutor.execute(context, pipeline);
    }
}