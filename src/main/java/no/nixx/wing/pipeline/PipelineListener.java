package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.WingPipelineParser;
import no.nixx.wing.antlr.WingPipelineParserBaseListener;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.Stack;

public class PipelineListener extends WingPipelineParserBaseListener {

    private Stack<Pipeline> pipelineStack = new Stack<>();

    private Stack<Command> commandStack = new Stack<>();

    public Pipeline getPipeline() {
        if (pipelineStack.size() != 1) {
            throw new IllegalStateException("Not balanced!");
        }
        return pipelineStack.pop();
    }

    private Pipeline getCurrentPipeline() {
        return pipelineStack.peek();
    }

    private Command getCurrentCommand() {
        return commandStack.peek();
    }

    @Override
    public void enterPipeline(@NotNull WingPipelineParser.PipelineContext ctx) {
        pipelineStack.push(new Pipeline());
    }

    @Override
    public void enterCmd(@NotNull WingPipelineParser.CmdContext ctx) {
        commandStack.push(new Command());
    }

    @Override
    public void exitCmd(@NotNull WingPipelineParser.CmdContext ctx) {
        getCurrentPipeline().addCommand(commandStack.pop());
    }

    @Override
    public void exitCs(@NotNull WingPipelineParser.CsContext ctx) {
        getCurrentCommand().addArgument(new CommandSubstitution(pipelineStack.pop()));
    }

    @Override
    public void exitVs(@NotNull WingPipelineParser.VsContext ctx) {
        getCurrentCommand().addArgument(new VariableSubstitution(ctx.VS_VARIABLE().getText()));
    }

    @Override
    public void exitLiteral(@NotNull WingPipelineParser.LiteralContext ctx) {
        getCurrentCommand().addArgument(new Literal(ctx.LT_TEXT().getText()));
    }

    @Override
    public void exitArg(@NotNull WingPipelineParser.ArgContext ctx) {
        if (ctx.ARG() != null) {
            getCurrentCommand().addArgument(new Literal(ctx.ARG().getText()));
        }
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
