package no.nixx.aslan.pipeline;

import no.nixx.aslan.antlr.AslanPipelineParser;
import no.nixx.aslan.antlr.AslanPipelineParserBaseListener;
import no.nixx.aslan.pipeline.model.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class PipelineListener extends AslanPipelineParserBaseListener {

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
    public void enterPipeline(@NotNull AslanPipelineParser.PipelineContext ctx) {
        pipelineStack.push(new Pipeline());
    }

    @Override
    public void enterCmd(@NotNull AslanPipelineParser.CmdContext ctx) {
        commandStack.push(new Command());
    }

    @Override
    public void exitCmd(@NotNull AslanPipelineParser.CmdContext ctx) {
        getCurrentPipeline().addCommand(commandStack.pop());
    }

    @Override
    public void exitCs(@NotNull AslanPipelineParser.CsContext ctx) {
        final CommandSubstitution cs = new CommandSubstitution(pipelineStack.pop());
        if (inQuotedString()) {
            quotedString.addComponent(cs);
        } else {
            getCurrentCommand().addArgument(cs);
        }
    }

    @Override
    public void exitVs(@NotNull AslanPipelineParser.VsContext ctx) {
        final VariableSubstitution vs = new VariableSubstitution(ctx.VS_VARIABLE().getText());
        if (inQuotedString()) {
            quotedString.addComponent(vs);
        } else {
            getCurrentCommand().addArgument(vs);
        }
    }

    @Override
    public void exitLiteral(@NotNull AslanPipelineParser.LiteralContext ctx) {
        getCurrentCommand().addArgument(new Literal(ctx.LT_TEXT().getText()));
    }

    @Override
    public void exitArg(@NotNull AslanPipelineParser.ArgContext ctx) {
        if (ctx.ARG() != null) {
            getCurrentCommand().addArgument(new Literal(ctx.ARG().getText()));
        }
    }

    // No support for nested quoted strings at this point

    @Override
    public void enterString(@NotNull AslanPipelineParser.StringContext ctx) {
        quotedString = new QuotedString();
        commandsCurrentlyInQuotedString.add(getCurrentCommand());
    }

    @Override
    public void exitString(@NotNull AslanPipelineParser.StringContext ctx) {
        getCurrentCommand().addArgument(quotedString);
        quotedString = null;
        commandsCurrentlyInQuotedString.remove(getCurrentCommand());
    }

    @Override
    public void exitText(@NotNull AslanPipelineParser.TextContext ctx) {
        quotedString.appendText(ctx.getText()); // TODO: Not sure if this is right...
    }

    private boolean inQuotedString() {
        // Must compare with ==, not contains()
        // Contains() calls equals() that might return true for two commands that are equal but not the same instance
        return commandsCurrentlyInQuotedString.stream().anyMatch((Command c) -> c == getCurrentCommand());
    }
}
