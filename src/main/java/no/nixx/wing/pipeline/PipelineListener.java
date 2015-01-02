package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.WingPipelineParser;
import no.nixx.wing.antlr.WingPipelineParserBaseListener;
import no.nixx.wing.pipeline.model.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class PipelineListener extends WingPipelineParserBaseListener {

    private Stack<Pipeline> pipelineStack = new Stack<>();

    private Stack<Command> commandStack = new Stack<>();

    private Set<Command> commandsCurrentlyInQuotedString = new HashSet<>();

    private QuotedString quotedString;

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
        final CommandSubstitution cs = new CommandSubstitution(pipelineStack.pop());
        if (inQuotedString()) {
            quotedString.addComponent(cs);
        } else {
            getCurrentCommand().addArgument(cs);
        }
    }

    @Override
    public void exitVs(@NotNull WingPipelineParser.VsContext ctx) {
        final VariableSubstitution vs = new VariableSubstitution(ctx.VS_VARIABLE().getText());
        if (inQuotedString()) {
            quotedString.addComponent(vs);
        } else {
            getCurrentCommand().addArgument(vs);
        }
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

    // No support for nested quoted strings at this point

    @Override
    public void enterString(@NotNull WingPipelineParser.StringContext ctx) {
        quotedString = new QuotedString();
        commandsCurrentlyInQuotedString.add(getCurrentCommand());
    }

    @Override
    public void exitString(@NotNull WingPipelineParser.StringContext ctx) {
        getCurrentCommand().addArgument(quotedString);
        quotedString = null;
        commandsCurrentlyInQuotedString.remove(getCurrentCommand());
    }

    @Override
    public void exitText(@NotNull WingPipelineParser.TextContext ctx) {
        quotedString.appendText(ctx.getText()); // TODO: Not sure if this is right...
    }

    private boolean inQuotedString() {
        // Must compare with ==, not contains()
        // Contains() calls equals() that might return true for two commands that are equal but not the same instance
        return commandsCurrentlyInQuotedString.stream().anyMatch((Command c) -> c == getCurrentCommand());
    }
}
