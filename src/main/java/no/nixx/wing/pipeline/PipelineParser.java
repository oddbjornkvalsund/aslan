package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.WingPipelineLexer;
import no.nixx.wing.antlr.WingPipelineParser;
import no.nixx.wing.pipeline.model.Pipeline;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;

import static java.lang.String.format;

public class PipelineParser {

    public Pipeline parseCommand(String cmd) {
        final WingPipelineLexer lexer = new WingPipelineLexer(new ANTLRInputStream(cmd));
        final WingPipelineParser parser = new WingPipelineParser(new BufferedTokenStream(lexer));
        final PipelineListener pipelineListener = new PipelineListener();
        parser.removeErrorListeners();
        parser.addParseListener(pipelineListener);
        parser.addErrorListener(new ErrorListener());
        parser.pipeline();

        return pipelineListener.getPipeline();
    }

    private class ErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            final String errorMsg;
            if (offendingSymbol instanceof CommonToken) {
                final CommonToken token = (CommonToken) offendingSymbol;
                final TokenSource tokenSource = token.getTokenSource();
                final CharStream inputStream = tokenSource.getInputStream();
                errorMsg = inputStream.getText(new Interval(0, inputStream.size()));
            } else {
                errorMsg = msg;
            }

            throw new ParseException(format("Syntax error at position %d: %s", charPositionInLine, errorMsg), e);
        }
    }
}