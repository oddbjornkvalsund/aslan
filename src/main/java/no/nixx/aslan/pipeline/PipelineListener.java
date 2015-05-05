package no.nixx.aslan.pipeline;

import no.nixx.aslan.antlr.AslanPipelineParser;
import no.nixx.aslan.antlr.AslanPipelineParserBaseListener;
import no.nixx.aslan.pipeline.model.*;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
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
        if (pipelineStack.size() == 1) {
            final Pipeline pipeline = pipelineStack.pop();
            removeUnnecessaryCompositeArguments(pipeline);
            return pipeline;
        } else {
            throw new IllegalStateException("Not balanced!");
        }
    }

    // By default all arguments are added to the command as composite arguments. This method reduces the arguments to
    // simpler argument types, if possible.
    private void removeUnnecessaryCompositeArguments(Pipeline pipeline) {
        for (Command command : pipeline.getCommandsUnmodifiable()) {
            for (Argument argument : command.getArgumentsUnmodifiable()) {
                if (argument.isRenderableTextAvailableWithoutCommmandExecution()) {
                    final Literal literal = new Literal(argument.getRenderableText());
                    literal.startIndex = argument.startIndex;
                    literal.stopIndex = argument.stopIndex;
                    literal.unprocessedArgument = argument.unprocessedArgument;
                    command.replaceArgument(argument, literal);
                } else if (argument.isCompositeArgument()) {
                    final CompositeArgument compositeArgument = (CompositeArgument) argument;
                    for (Argument ca : compositeArgument) {
                        if (ca.isQuotedString()) {
                            final QuotedString quotedString = (QuotedString) ca;
                            for (QuotedString.Component component : quotedString.getComponentsUnmodifiable()) {
                                if (component.argument.isCommandSubstitution()) {
                                    final CommandSubstitution cs = (CommandSubstitution) component.argument;
                                    removeUnnecessaryCompositeArguments(cs.getPipeline());
                                } else if (component.argument.isCompositeArgument()) {
                                    throw new IllegalStateException("Directly nested composite arguments should not be possible, this is a bug.");
                                }
                            }
                        } else if (ca.isCommandSubstitution()) {
                            final CommandSubstitution cs = (CommandSubstitution) ca;
                            removeUnnecessaryCompositeArguments(cs.getPipeline());
                        }
                    }

                    if (compositeArgument.size() == 1) {
                        command.replaceArgument(argument, compositeArgument.get(0));
                    }
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
            final CompositeArgument argument = compositeArgumentStack.pop();
            setTokenProperties(ctx, argument);
            getCurrentCommand().addArgument(argument);
            compositeArgumentStack.push(new CompositeArgument());
        }
    }

    @Override
    public void exitCs(@NotNull AslanPipelineParser.CsContext ctx) {
        final CommandSubstitution cs = new CommandSubstitution(pipelineStack.pop());
        setTokenProperties(ctx, cs);
        if (inQuotedString()) {
            quotedString.addComponent(cs);
        } else {
            getCurrentCompositeArgument().addArgument(cs);
        }
    }

    @Override
    public void exitVs(@NotNull AslanPipelineParser.VsContext ctx) {
        final VariableSubstitution vs = new VariableSubstitution(ctx.VS_VARIABLE().getText());
        setTokenProperties(ctx, vs);
        if (inQuotedString()) {
            quotedString.addComponent(vs);
        } else {
            getCurrentCompositeArgument().addArgument(vs);
        }
    }

    @Override
    public void exitLiteral(@NotNull AslanPipelineParser.LiteralContext ctx) {
        final Literal argument = new Literal(ctx.LT_TEXT().getText());
        setTokenProperties(ctx, argument);
        getCurrentCompositeArgument().addArgument(argument);
    }

    @Override
    public void exitArg(@NotNull AslanPipelineParser.ArgContext ctx) {
        // This awkward test is here just because there is a lexer token "ARG" and a parser token "arg"
        if (ctx.ARG() != null) {
            final Literal argument = new Literal(ctx.ARG().getText());
            setTokenProperties(ctx, argument);
            getCurrentCompositeArgument().addArgument(argument);
        }
    }

    @Override
    public void exitCmd(@NotNull AslanPipelineParser.CmdContext ctx) {
        final CompositeArgument argument = compositeArgumentStack.pop();
        if (!argument.isEmpty()) {
            setTokenProperties(ctx, argument);
            getCurrentCommand().addArgument(argument);
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
        setTokenProperties(ctx, quotedString);
        getCurrentCompositeArgument().addArgument(quotedString);
        quotedString = null;
        commandsCurrentlyInQuotedString.remove(getCurrentCommand());
    }

    @Override
    public void exitText(@NotNull AslanPipelineParser.TextContext ctx) {
        quotedString.appendText(ctx.getText());
    }

    private void setTokenProperties(ParserRuleContext ctx, Argument argument) {
        final CommonToken start = (CommonToken) ctx.getStart();
        final CommonToken stop = (CommonToken) ctx.getStop();
        final String unprocessedArgument = start.getTokenSource().getInputStream().getText(new Interval(start.getStartIndex(), stop.getStopIndex()));
        argument.startIndex = start.getStartIndex();
        argument.stopIndex = stop.getStopIndex();
        argument.unprocessedArgument = unprocessedArgument;
    }

    private void setTokenProperties(ParserRuleContext ctx, CompositeArgument argument) {
        final CommonToken start = (CommonToken) ctx.getStart();
        argument.startIndex = argument.get(0).startIndex;
        argument.stopIndex = argument.get(argument.size() - 1).stopIndex;
        argument.unprocessedArgument = start.getTokenSource().getInputStream().getText(new Interval(argument.startIndex, argument.stopIndex));
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
