package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.WingPipelineLexer;
import no.nixx.wing.antlr.WingPipelineParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;

public class PipelineParser {

    public Pipeline parseCommand(String cmd) {
        final WingPipelineLexer lexer = new WingPipelineLexer(new ANTLRInputStream(cmd));
        final WingPipelineParser parser = new WingPipelineParser(new BufferedTokenStream(lexer));
        final PipelineListener pipelineListener = new PipelineListener();
        parser.addParseListener(pipelineListener);
        parser.pipeline();

        return pipelineListener.getPipeline();
    }

}