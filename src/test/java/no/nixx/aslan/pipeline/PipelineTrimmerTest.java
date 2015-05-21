package no.nixx.aslan.pipeline;

import no.nixx.aslan.pipeline.model.*;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineTrimmerTest {

    final PipelineTrimmer pipelineTrimmer = new PipelineTrimmer();

    @Test(expected = IllegalArgumentException.class)
    public void testNull() {
        pipelineTrimmer.trim(null);
    }

    @Test
    public void testEmptyPipeline() {
        final Pipeline pipeline = new Pipeline();
        final Pipeline trimmedPipeline = pipelineTrimmer.trim(pipeline);
        assertEquals(trimmedPipeline.getCommands().size(), 0);
    }

    @Test
    public void testEmptyCommand() {
        final Pipeline pipeline = new Pipeline(new Command());

        final Pipeline trimmedPipeline = pipelineTrimmer.trim(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(pipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 0);
    }

    @Test
    public void testUnrenderableSingleArgumentInCompositeArgumentIsUnwrapped() {
        final Pipeline pipeline = new Pipeline(new Command(compositeArgument(commandSubstitution("cs"))));

        final Pipeline trimmedPipeline = pipelineTrimmer.trim(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isCommandSubstitution());
    }

    @Test
    public void testRenderableArgumentsInCompositeArgumentAreCollapsed() {
        final Pipeline pipeline = new Pipeline(new Command(compositeArgument("1", "2", "3")));

        final Pipeline trimmedPipeline = pipelineTrimmer.trim(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isLiteral());
        assertEquals(trimmedArgument.getRenderedText(), "123");
    }

    @Test
    public void testNestedPipelinesAreTrimmed() {
        final Pipeline innerPipeline = new Pipeline(new Command(compositeArgument("1", "2")));
        final Pipeline outerPipeline = new Pipeline(new Command(compositeArgument(new CommandSubstitution(innerPipeline, 0, 0, ""))));

        final Pipeline trimmedPipeline = pipelineTrimmer.trim(outerPipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isCommandSubstitution());

        final CommandSubstitution trimmedCommandSubstitution = (CommandSubstitution) trimmedArgument;
        assertNumberOfCommands(trimmedCommandSubstitution.getPipeline(), 1);

        final Command trimmedCommandSubstitutionCommand = firstOf(trimmedCommandSubstitution.getPipeline().getCommands());
        assertNumberOfArguments(trimmedCommandSubstitutionCommand, 1);

        final Argument trimmedCommandSubstitutionArgument = firstOf(trimmedCommandSubstitutionCommand.getArguments());
        assertTrue(trimmedCommandSubstitutionArgument.isLiteral());
        assertEquals(trimmedCommandSubstitutionArgument.getRenderedText(), "12");
    }

    @Test
    public void testNestedPipelinesNotWrappedInCompositeArgumentsAreTrimmed() {
        final Pipeline innerPipeline = new Pipeline(new Command(compositeArgument("1", "2")));
        final Pipeline outerPipeline = new Pipeline(new Command(new CommandSubstitution(innerPipeline, 0, 0, "")));

        final Pipeline trimmedPipeline = pipelineTrimmer.trim(outerPipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isCommandSubstitution());

        final CommandSubstitution trimmedCommandSubstitution = (CommandSubstitution) trimmedArgument;
        assertNumberOfCommands(trimmedCommandSubstitution.getPipeline(), 1);

        final Command trimmedCommandSubstitutionCommand = firstOf(trimmedCommandSubstitution.getPipeline().getCommands());
        assertNumberOfArguments(trimmedCommandSubstitutionCommand, 1);

        final Argument trimmedCommandSubstitutionArgument = firstOf(trimmedCommandSubstitutionCommand.getArguments());
        assertTrue(trimmedCommandSubstitutionArgument.isLiteral());
        assertEquals(trimmedCommandSubstitutionArgument.getRenderedText(), "12");
    }

    private void assertNumberOfCommands(Pipeline pipeline, int expectedNumberOfCommands) {
        assertEquals(pipeline.getCommands().size(), expectedNumberOfCommands);
    }

    private void assertNumberOfArguments(Command command, int expectedNumberOfArguments) {
        assertEquals(command.getArguments().size(), expectedNumberOfArguments);
    }

    private CompositeArgument compositeArgument(String... strings) {
        final List<Argument> argumentList = Stream.of(strings).map(s -> new Literal(s, 0, 0, s)).collect(toList());
        return compositeArgument(argumentList.toArray(new Argument[argumentList.size()]));
    }

    private CompositeArgument compositeArgument(Argument... arguments) {
        return new CompositeArgument(asList(arguments), 0, 0, "");
    }

    private CommandSubstitution commandSubstitution(String text) {
        return new CommandSubstitution(new Pipeline(new Command(new Literal(text, 0, 0, text))), 0, 0, "$(" + text + ")");
    }

}