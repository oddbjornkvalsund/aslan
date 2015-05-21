package no.nixx.aslan.pipeline;

import no.nixx.aslan.antlr.AslanPipelineParser;
import no.nixx.aslan.antlr.AslanPipelineParserBaseListener;
import no.nixx.aslan.pipeline.model.Argument;
import no.nixx.aslan.pipeline.model.Command;
import no.nixx.aslan.pipeline.model.CommandSubstitution;
import no.nixx.aslan.pipeline.model.CompositeArgument;
import no.nixx.aslan.pipeline.model.Literal;
import no.nixx.aslan.pipeline.model.Pipeline;
import no.nixx.aslan.pipeline.model.QuotedString;
import no.nixx.aslan.pipeline.model.VariableSubstitution;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class PipelineListener extends AslanPipelineParserBaseListener {

    private Stack<Pipeline> pipelineStack = new Stack<>();

    private Stack<Command> commandStack = new Stack<>();

    private Stack<CompositeArgumentCollector> compositeArgumentStack = new Stack<>();

    private Set<Command> commandsCurrentlyInQuotedString = new HashSet<>();

    private QuotedStringCollector quotedStringCollector;

    public Pipeline getPipeline() {
        if (pipelineStack.size() == 1) {
            final Pipeline pipeline = pipelineStack.pop();
            final PipelineTrimmer pipelineTrimmer = new PipelineTrimmer();
            return pipelineTrimmer.getTrimmedPipeline(pipeline);
        } else {
            throw new IllegalStateException("Not balanced!");
        }
    }

    private Command getCurrentCommand() {
        return commandStack.peek();
    }

    private CompositeArgumentCollector getCurrentCompositeArgument() {
        return compositeArgumentStack.peek();
    }

    @Override
    public void enterPipeline(@NotNull AslanPipelineParser.PipelineContext ctx) {
        pipelineStack.push(new Pipeline());
    }

    @Override
    public void enterCmd(@NotNull AslanPipelineParser.CmdContext ctx) {
        commandStack.push(new Command());
        compositeArgumentStack.push(new CompositeArgumentCollector());
    }

    @Override
    public void enterSpace(@NotNull AslanPipelineParser.SpaceContext ctx) {
        if (inCompositeArgument()) {
            final CompositeArgumentCollector compositeArgumentCollector = compositeArgumentStack.pop();
            addArgumentToCurrentCommand(compositeArgumentCollector.getCompositeArgument(ctx));
            compositeArgumentStack.push(new CompositeArgumentCollector());
        }
    }

    @Override
    public void exitCs(@NotNull AslanPipelineParser.CsContext ctx) {
        final TokenProperties tp = getTokenProperties(ctx);
        final CommandSubstitution cs = new CommandSubstitution(pipelineStack.pop(), tp.startIndex, tp.stopIndex, tp.unprocessedArgument);
        if (inQuotedString()) {
            quotedStringCollector.addComponent(cs);
        } else {
            getCurrentCompositeArgument().addArgument(cs);
        }
    }

    @Override
    public void exitVs(@NotNull AslanPipelineParser.VsContext ctx) {
        final TokenProperties tp = getTokenProperties(ctx);
        final VariableSubstitution vs = new VariableSubstitution(ctx.VS_VARIABLE().getText(), tp.startIndex, tp.stopIndex, tp.unprocessedArgument);
        if (inQuotedString()) {
            quotedStringCollector.addComponent(vs);
        } else {
            getCurrentCompositeArgument().addArgument(vs);
        }
    }

    @Override
    public void exitLiteral(@NotNull AslanPipelineParser.LiteralContext ctx) {
        final TokenProperties tp = getTokenProperties(ctx);
        final Literal literal = new Literal(ctx.LT_TEXT().getText(), tp.startIndex, tp.stopIndex, tp.unprocessedArgument);
        getCurrentCompositeArgument().addArgument(literal);
    }

    @Override
    public void exitArg(@NotNull AslanPipelineParser.ArgContext ctx) {
        // This awkward test is here just because there is a lexer token "ARG" and a parser token "arg"
        if (ctx.ARG() != null) {
            final TokenProperties tp = getTokenProperties(ctx);
            final Literal literal = new Literal(ctx.ARG().getText(), tp.startIndex, tp.stopIndex, tp.unprocessedArgument);
            getCurrentCompositeArgument().addArgument(literal);
        }
    }

    @Override
    public void exitCmd(@NotNull AslanPipelineParser.CmdContext ctx) {
        final CompositeArgumentCollector compositeArgumentCollector = compositeArgumentStack.pop();
        if (!compositeArgumentCollector.isEmpty()) {
            addArgumentToCurrentCommand(compositeArgumentCollector.getCompositeArgument(ctx));
        }
        addCommandToCurrentPipeline(commandStack.pop());
    }

    @Override
    public void enterString(@NotNull AslanPipelineParser.StringContext ctx) {
        quotedStringCollector = new QuotedStringCollector();
        commandsCurrentlyInQuotedString.add(getCurrentCommand());
    }

    @Override
    public void exitString(@NotNull AslanPipelineParser.StringContext ctx) {
        getCurrentCompositeArgument().addArgument(quotedStringCollector.getQuotedString(ctx));
        quotedStringCollector = null;
        commandsCurrentlyInQuotedString.remove(getCurrentCommand());
    }

    @Override
    public void exitText(@NotNull AslanPipelineParser.TextContext ctx) {
        quotedStringCollector.appendText(ctx.getText());
    }

    // No support for nested quoted strings at this point

    private TokenProperties getTokenProperties(ParserRuleContext ctx) {
        final CommonToken start = (CommonToken) ctx.getStart();
        final CommonToken stop = (CommonToken) ctx.getStop();
        final String unprocessedArgument = ctx.getText();

        return new TokenProperties(start.getStartIndex(), stop.getStopIndex() + 1, unprocessedArgument);
    }

    private boolean inCompositeArgument() {
        return !getCurrentCompositeArgument().isEmpty();
    }

    private void addArgumentToCurrentCommand(Argument argument) {
        commandStack.push(commandStack.pop().addArgument(argument));
    }

    private void addCommandToCurrentPipeline(Command command) {
        pipelineStack.push(pipelineStack.pop().addCommand(command));
    }

    private boolean inQuotedString() {
        // Must compare with ==, not contains()
        // Contains() calls equals() that might return true for two commands that are equal but not the same instance
//        return commandsCurrentlyInQuotedString.stream().anyMatch((Command c) -> c == getCurrentCommand());
        return commandsCurrentlyInQuotedString.stream().anyMatch((Command c) -> c.getIdentity() == getCurrentCommand().getIdentity());
    }

    private class CompositeArgumentCollector {

        final List<Argument> arguments = new ArrayList<>();

        public void addArgument(Argument argument) {
            arguments.add(checkNotNull(argument));
        }

        public boolean isEmpty() {
            return arguments.isEmpty();
        }

        public CompositeArgument getCompositeArgument(ParserRuleContext ctx) {
            final CommonToken start = (CommonToken) ctx.getStart();
            final int startIndex = arguments.get(0).getStartIndex();
            final int stopIndex = arguments.get(arguments.size() - 1).getStopIndex();
            final String unprocessedArgument = start.getTokenSource().getInputStream().getText(new Interval(startIndex, stopIndex - 1));

            return new CompositeArgument(arguments, startIndex, stopIndex, unprocessedArgument);
        }
    }

    private class QuotedStringCollector {

        final StringBuilder sb = new StringBuilder();
        final List<QuotedString.Component> components = new ArrayList<>();

        public void appendText(String text) {
            sb.append(checkNotNull(text));
        }

        public void addComponent(Argument argument) {
            checkNotNull(argument);
            if (argument.isCommandSubstitution() || argument.isVariableSubstitution()) {
                components.add(new QuotedString.Component(sb.length(), argument));
            } else {
                throw new IllegalArgumentException("Invalid argument type: " + argument);
            }
        }

        public QuotedString getQuotedString(ParserRuleContext ctx) {
            final TokenProperties properties = getTokenProperties(ctx);
            return new QuotedString(sb.toString(), components, properties.startIndex, properties.stopIndex, properties.unprocessedArgument);
        }
    }

    private class TokenProperties {
        final int startIndex;
        final int stopIndex;
        final String unprocessedArgument;

        public TokenProperties(int startIndex, int stopIndex, String unprocessedArgument) {
            this.startIndex = startIndex;
            this.stopIndex = stopIndex;
            this.unprocessedArgument = unprocessedArgument;
        }
    }
}