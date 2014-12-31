package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.WingPipelineParser;
import no.nixx.wing.antlr.WingPipelineParserBaseListener;
import org.antlr.v4.runtime.misc.NotNull;

public class PipelineListener extends WingPipelineParserBaseListener {

    final Pipeline pipeline = new Pipeline();

    private Command currentCommand; // TODO: This must be a Stack<Command>

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void enterCmd(@NotNull WingPipelineParser.CmdContext ctx) {
        currentCommand = new Command();
    }

    @Override
    public void exitCmd(@NotNull WingPipelineParser.CmdContext ctx) {
        pipeline.addCommand(currentCommand);
    }

    @Override
    public void exitArg(@NotNull WingPipelineParser.ArgContext ctx) {
        if (ctx.ARG() != null) {
            currentCommand.addArgument(new Literal(ctx.ARG().getText()));
        }
    }

    @Override
    public void exitLiteral(@NotNull WingPipelineParser.LiteralContext ctx) {
        currentCommand.addArgument(new Literal(ctx.LT_TEXT().getText()));
    }

    @Override
    public void exitVs(@NotNull WingPipelineParser.VsContext ctx) {
        currentCommand.addArgument(new VariableSubstitution(ctx.VS_VARIABLE().getText()));
    }

    @Override
    public void enterCs(@NotNull WingPipelineParser.CsContext ctx) {
        // TODO
    }

    @Override
    public void exitCs(@NotNull WingPipelineParser.CsContext ctx) {
        // TODO
    }

    @Override
    public void enterText(@NotNull WingPipelineParser.TextContext ctx) {
        // TODO
    }

    @Override
    public void exitText(@NotNull WingPipelineParser.TextContext ctx) {
        // TODO
    }
}
