package no.nixx.aslan.pipeline;

import no.nixx.aslan.core.utils.Preconditions;
import no.nixx.aslan.pipeline.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * By default all components are added to the command as composite arguments. This class reduces the components to
 * simpler argument types if possible.
 */
public class PipelineTrimmer {

    public Pipeline getTrimmedPipeline(Pipeline pipeline) {
        if (pipeline == null) {
            throw new IllegalArgumentException("Pipeline cannot be null!");
        } else {
            return new Pipeline(getTrimmedCommands(pipeline.getCommands()));
        }
    }

    @SuppressWarnings("Convert2streamapi")
    private List<Command> getTrimmedCommands(List<Command> commands) {
        final List<Command> trimmedCommands = new ArrayList<>();
        for (Command command : commands) {
            trimmedCommands.add(new Command(command, getTrimmedArguments(command.getArguments())));
        }
        return trimmedCommands;
    }

    private List<Argument> getTrimmedArguments(List<Argument> arguments) {
        final List<Argument> trimmedArguments = new ArrayList<>();
        for (Argument arg : arguments) {
            if (arg.isRenderable()) {
                trimmedArguments.add(getTrimmedRenderable(arg));
            } else if (arg.isQuotedString()) {
                final QuotedString quotedString = (QuotedString) arg;
                final QuotedString trimmedQuotedString = getTrimmedQuotedString(quotedString);
                trimmedArguments.add(trimmedQuotedString);
            } else if (arg.isCompositeArgument()) {
                final CompositeArgument compositeArgument = (CompositeArgument) arg;
                final CompositeArgument trimmedCompositeArgument = getTrimmedCompositeArgument(compositeArgument);
                trimmedArguments.add((trimmedCompositeArgument.size() == 1) ? firstOf(trimmedCompositeArgument) : trimmedCompositeArgument);
            } else if (arg.isCommandSubstitution()) {
                final CommandSubstitution commandSubstitution = (CommandSubstitution) arg;
                final CommandSubstitution trimmedCommandSubstitution = getTrimmedCommandSubstitution(commandSubstitution);
                trimmedArguments.add(trimmedCommandSubstitution);
            } else {
                trimmedArguments.add(arg);
            }
        }
        return trimmedArguments;
    }

    private QuotedString getTrimmedQuotedString(QuotedString quotedString) {
        final List<QuotedString.Component> trimmedComponents = new ArrayList<>();
        for (QuotedString.Component component : quotedString.getComponents()) {
            if (component.argument.isCommandSubstitution()) {
                final CommandSubstitution commandSubstitution = (CommandSubstitution) component.argument;
                final CommandSubstitution trimmedCommandSubstitution = getTrimmedCommandSubstitution(commandSubstitution);
                trimmedComponents.add(new QuotedString.Component(component.position, trimmedCommandSubstitution));
            } else if (component.argument.isCompositeArgument()) {
                final CompositeArgument compositeArgument = (CompositeArgument) component.argument;
                final CompositeArgument trimmedCompositeArgument = getTrimmedCompositeArgument(compositeArgument);
                trimmedComponents.add(new QuotedString.Component(component.position, trimmedCompositeArgument));
            } else {
                trimmedComponents.add(component);
            }
        }
        return new QuotedString(quotedString.getText(), trimmedComponents, quotedString.getProperties());
    }

    private CompositeArgument getTrimmedCompositeArgument(CompositeArgument ca) {
        return new CompositeArgument(getTrimmedArguments(ca.getArguments()), ca.getProperties());
    }

    private CommandSubstitution getTrimmedCommandSubstitution(CommandSubstitution cs) {
        return new CommandSubstitution(getTrimmedPipeline(cs.getPipeline()), cs.getProperties());
    }

    private Literal getTrimmedRenderable(Argument argument) {
        Preconditions.checkArgument(argument.isRenderable());
        return new Literal(argument.getRenderedText(), argument.getProperties());
    }

    private <T> T firstOf(Iterable<T> iterable) {
        return iterable.iterator().next();
    }
}