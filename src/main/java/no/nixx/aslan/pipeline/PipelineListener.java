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

    private Stack<CompositeArgument> compositeArgumentStack = new Stack<>();

    private Set<Command> commandsCurrentlyInQuotedString = new HashSet<>();

    private QuotedString quotedString;

    public Pipeline getPipeline() {
        if (pipelineStack.size() != 1) {
            throw new IllegalStateException("Not balanced!");
        }

        final Pipeline pipeline = pipelineStack.pop();
        reduceComplexArguments(pipeline);

        return pipeline;
    }

    private void reduceComplexArguments(Pipeline pipeline) {
        for (Command command : pipeline.getCommandsUnmodifiable()) {
            for (Argument argument : command.getArgumentsUnmodifiable()) {
                if (argument.isRenderableTextAvailableWithoutCommmandExecution()) {
                    command.replaceArgument(argument, new Literal(argument.getRenderableText()));
                }
            }
        }
    }

    private Pipeline getCurrentPipeline() {
        return pipelineStack.peek();
    }

    private Command getCurrentCommand() {
        return commandStack.peek();
    }

    private CompositeArgument getCurrentCompositeArgument() {
        return compositeArgumentStack.peek();
    }

    @Override
    public void enterPipeline(@NotNull AslanPipelineParser.PipelineContext ctx) {
        pipelineStack.push(new Pipeline());
    }

    @Override
    public void enterCmd(@NotNull AslanPipelineParser.CmdContext ctx) {
        commandStack.push(new Command());
        compositeArgumentStack.push(new CompositeArgument());
    }

    @Override
    public void enterSpace(@NotNull AslanPipelineParser.SpaceContext ctx) {
        if (inCompositeArgument()) {
            getCurrentCommand().addArgument(compositeArgumentStack.pop());
            compositeArgumentStack.push(new CompositeArgument());
        }
    }

    @Override
    public void exitCs(@NotNull AslanPipelineParser.CsContext ctx) {
        final CommandSubstitution cs = new CommandSubstitution(pipelineStack.pop());
        if (inQuotedString()) {
            quotedString.addComponent(cs);
        } else {
            getCurrentCompositeArgument().addArgument(cs);
        }
    }

    @Override
    public void exitVs(@NotNull AslanPipelineParser.VsContext ctx) {
        final VariableSubstitution vs = new VariableSubstitution(ctx.VS_VARIABLE().getText());
        if (inQuotedString()) {
            quotedString.addComponent(vs);
        } else {
            getCurrentCompositeArgument().addArgument(vs);
        }
    }

    @Override
    public void exitLiteral(@NotNull AslanPipelineParser.LiteralContext ctx) {
        getCurrentCompositeArgument().addArgument(new Literal(ctx.LT_TEXT().getText()));
    }

    @Override
    public void exitArg(@NotNull AslanPipelineParser.ArgContext ctx) {
        // This awkward test is here just because there is a lexer token "ARG" and a parser token "arg"
        if (ctx.ARG() != null) {
            getCurrentCompositeArgument().addArgument(new Literal(ctx.ARG().getText()));
        }
    }

    @Override
    public void exitCmd(@NotNull AslanPipelineParser.CmdContext ctx) {
        final CompositeArgument compositeArgument = compositeArgumentStack.pop();
        if (!compositeArgument.isEmpty()) {
            getCurrentCommand().addArgument(compositeArgument);
        }
        getCurrentPipeline().addCommand(commandStack.pop());
    }

    // No support for nested quoted strings at this point

    @Override
    public void enterString(@NotNull AslanPipelineParser.StringContext ctx) {
        quotedString = new QuotedString();
        commandsCurrentlyInQuotedString.add(getCurrentCommand());
    }

    @Override
    public void exitString(@NotNull AslanPipelineParser.StringContext ctx) {
        getCurrentCompositeArgument().addArgument(quotedString);
        quotedString = null;
        commandsCurrentlyInQuotedString.remove(getCurrentCommand());
    }

    @Override
    public void exitText(@NotNull AslanPipelineParser.TextContext ctx) {
        quotedString.appendText(ctx.getText());
    }

    private boolean inCompositeArgument() {
        return !getCurrentCompositeArgument().isEmpty();
    }

    private boolean inQuotedString() {
        // Must compare with ==, not contains()
        // Contains() calls equals() that might return true for two commands that are equal but not the same instance
        return commandsCurrentlyInQuotedString.stream().anyMatch((Command c) -> c == getCurrentCommand());
    }
}
