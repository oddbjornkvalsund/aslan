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

    public Pipeline trim(Pipeline pipeline) {
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

    private CompositeArgument getTrimmedCompositeArgument(CompositeArgument compositeArgument) {
        final List<Argument> trimmedCompositeArguments = new ArrayList<>();

        for (Argument arg : compositeArgument) {
            if (arg.isQuotedString()) {
                final QuotedString quotedString = (QuotedString) arg;
                final QuotedString trimmedQuotedString = getTrimmedQuotedString(quotedString);
                trimmedCompositeArguments.add(trimmedQuotedString);
            } else if (arg.isCommandSubstitution()) {
                final CommandSubstitution commandSubstitution = (CommandSubstitution) arg;
                final CommandSubstitution trimmedCommandSubstitution = getTrimmedCommandSubstitution(commandSubstitution);
                trimmedCompositeArguments.add(trimmedCommandSubstitution);
            } else {
                trimmedCompositeArguments.add(arg);
            }
        }

        return new CompositeArgument(trimmedCompositeArguments, compositeArgument.getStartIndex(), compositeArgument.getStopIndex(), compositeArgument.getUnprocessedArgument());
    }

    private CommandSubstitution getTrimmedCommandSubstitution(CommandSubstitution commmandSubstitution) {
        final Pipeline trimmedPipeline = trim(commmandSubstitution.getPipeline());
        return new CommandSubstitution(trimmedPipeline, commmandSubstitution.getStartIndex(), commmandSubstitution.getStopIndex(), commmandSubstitution.getUnprocessedArgument());
    }

    private QuotedString getTrimmedQuotedString(QuotedString quotedString) {
        final List<QuotedString.Component> trimmedComponents = new ArrayList<>();
        for (QuotedString.Component component : quotedString.getComponents()) {
            if (component.argument.isCommandSubstitution()) {
                final CommandSubstitution commandSubstitution = (CommandSubstitution) component.argument;
                final CommandSubstitution trimmedCommandSubstitution = getTrimmedCommandSubstitution(commandSubstitution);
                trimmedComponents.add(new QuotedString.Component(component.position, trimmedCommandSubstitution));
            } else if (component.argument.isCompositeArgument()) {
                throw new IllegalStateException("Directly nested composite components should not be possible, this is a bug.");
            } else {
                trimmedComponents.add(component);
            }
        }
        return new QuotedString(quotedString.getText(), trimmedComponents, quotedString.getStartIndex(), quotedString.getStopIndex(), quotedString.getUnprocessedArgument());
    }

    private Literal getTrimmedRenderable(Argument argument) {
        Preconditions.checkArgument(argument.isRenderable());
        return new Literal(argument.getRenderedText(), argument.getStartIndex(), argument.getStopIndex(), argument.getUnprocessedArgument());
    }

    private <T> T firstOf(Iterable<T> iterable) {
        return iterable.iterator().next();
    }
}