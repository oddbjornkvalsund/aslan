package no.nixx.aslan.pipeline;

import no.nixx.aslan.pipeline.model.*;
import org.junit.Test;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineTrimmerTest {

    final PipelineTrimmer pipelineTrimmer = new PipelineTrimmer();

    @Test(expected = IllegalArgumentException.class)
    public void testNull() {
        pipelineTrimmer.getTrimmedPipeline(null);
    }

    @Test
    public void testEmptyPipeline() {
        final Pipeline pipeline = new Pipeline();
        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(pipeline);
        assertEquals(trimmedPipeline.getCommands().size(), 0);
    }

    @Test
    public void testEmptyCommand() {
        final Pipeline pipeline = new Pipeline(new Command());

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(pipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 0);
    }

    @Test
    public void testUnrenderableSingleArgumentInCompositeArgumentIsUnwrapped() {
        final Pipeline pipeline = new Pipeline(new Command(compositeArgument(commandSubstitution("cs"))));

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isCommandSubstitution());
    }

    @Test
    public void testRenderableArgumentsInCompositeArgumentAreCollapsed() {
        final Pipeline pipeline = new Pipeline(new Command(compositeArgument("1", "2", "3")));

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(pipeline);
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
        final Pipeline outerPipeline = new Pipeline(new Command(compositeArgument(new CommandSubstitution(innerPipeline, new ArgumentProperties(0, 0, "")))));

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(outerPipeline);
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
        final Pipeline outerPipeline = new Pipeline(new Command(new CommandSubstitution(innerPipeline, new ArgumentProperties(0, 0, ""))));

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(outerPipeline);
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
    public void testCompositeArgumentsInQuotedStringAreTrimmed() {
        // A composite argument within a quoted string is not really possible to express on the command line, but supported for completeness
        final CompositeArgument compositeArgument = compositeArgument("1", "2");
        final QuotedString.Component quotedStringComponent = new QuotedString.Component(4, compositeArgument);
        final QuotedString quotedString = new QuotedString("test", singletonList(quotedStringComponent), new ArgumentProperties(0, 0, ""));
        final Pipeline pipeline = new Pipeline(new Command(quotedString));

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isLiteral());
        assertEquals(trimmedArgument.getRenderedText(), "test12");
    }

    @Test
    public void testCompositeArgumentsWithCommandSubstitutionInQuotedStringAreTrimmed() {
        // A composite argument within a quoted string is not really possible to express on the command line, but supported for completeness
        final CommandSubstitution csWithCompositeArgument = new CommandSubstitution(new Pipeline(new Command(compositeArgument("1", "2"))), new ArgumentProperties(0, 0, ""));
        final CompositeArgument compositeArgument = compositeArgument(new Literal("test", new ArgumentProperties(0, 0, "test")), csWithCompositeArgument);
        final QuotedString.Component quotedStringComponent = new QuotedString.Component(4, compositeArgument);
        final Pipeline pipeline = new Pipeline(new Command(new QuotedString("test", singletonList(quotedStringComponent), new ArgumentProperties(0, 0, ""))));

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isQuotedString());

        final QuotedString trimmedQuotedString = (QuotedString) trimmedArgument;
        assertEquals(trimmedQuotedString.getText(), "test");
        assertEquals(1, trimmedQuotedString.getComponents().size());
        assertTrue(trimmedQuotedString.getComponents().get(0).argument.isCompositeArgument());

        final CompositeArgument qsCompositeArgument = (CompositeArgument) trimmedQuotedString.getComponents().get(0).argument;
        assertEquals(2, qsCompositeArgument.getArguments().size());
        assertTrue(qsCompositeArgument.getArguments().get(0).isLiteral());
        assertTrue(qsCompositeArgument.getArguments().get(1).isCommandSubstitution());

        final Literal qsCompositeArgumentLiteral = (Literal) qsCompositeArgument.getArguments().get(0);
        assertEquals("test", qsCompositeArgumentLiteral.getRenderedText());

        final CommandSubstitution qsCompositeArgumentCommandSubstitution = (CommandSubstitution) qsCompositeArgument.getArguments().get(1);
        assertTrue(qsCompositeArgumentCommandSubstitution.getPipeline().getCommands().get(0).getArguments().get(0).isLiteral());
        assertEquals("12", qsCompositeArgumentCommandSubstitution.getPipeline().getCommands().get(0).getArguments().get(0).getRenderedText());
    }

    @Test
    public void testNestedPipelineInQuotedStringIsTrimmed() {
        final CommandSubstitution csWithCompositeArgument = new CommandSubstitution(new Pipeline(new Command(compositeArgument("1", "2"))), new ArgumentProperties(0, 0, ""));
        final QuotedString.Component quotedStringComponent = new QuotedString.Component(4, csWithCompositeArgument);
        final QuotedString quotedString = new QuotedString("test", singletonList(quotedStringComponent), new ArgumentProperties(0, 0, ""));
        final Pipeline pipeline = new Pipeline(new Command(quotedString));

        final Pipeline trimmedPipeline = pipelineTrimmer.getTrimmedPipeline(pipeline);
        assertNumberOfCommands(trimmedPipeline, 1);

        final Command trimmedCommand = firstOf(trimmedPipeline.getCommands());
        assertNumberOfArguments(trimmedCommand, 1);

        final Argument trimmedArgument = firstOf(trimmedCommand.getArguments());
        assertTrue(trimmedArgument.isQuotedString());

        final QuotedString trimmedQuotedString = (QuotedString) trimmedArgument;
        assertEquals(trimmedQuotedString.getComponents().size(), 1);
        assertTrue(trimmedQuotedString.getComponents().get(0).argument.isCommandSubstitution());

        final CommandSubstitution trimmedQuotedStringCommandSubstitution = (CommandSubstitution) trimmedQuotedString.getComponents().get(0).argument;
        assertNumberOfCommands(trimmedQuotedStringCommandSubstitution.getPipeline(), 1);

        final Command trimmedQuotedStringCommandSubstitutionCommand = trimmedQuotedStringCommandSubstitution.getPipeline().getCommands().get(0);
        assertNumberOfArguments(trimmedQuotedStringCommandSubstitutionCommand, 1);
    }

    private void assertNumberOfCommands(Pipeline pipeline, int expectedNumberOfCommands) {
        assertEquals(pipeline.getCommands().size(), expectedNumberOfCommands);
    }

    private void assertNumberOfArguments(Command command, int expectedNumberOfArguments) {
        assertEquals(command.getArguments().size(), expectedNumberOfArguments);
    }

    private CompositeArgument compositeArgument(String... strings) {
        final List<Argument> argumentList = Stream.of(strings).map(s -> new Literal(s, new ArgumentProperties(0, 0, s))).collect(toList());
        return compositeArgument(argumentList.toArray(new Argument[argumentList.size()]));
    }

    private CompositeArgument compositeArgument(Argument... arguments) {
        return new CompositeArgument(asList(arguments), new ArgumentProperties(0, 0, ""));
    }

    private CommandSubstitution commandSubstitution(String text) {
        return new CommandSubstitution(new Pipeline(new Command(new Literal(text, new ArgumentProperties(0, 0, text)))), new ArgumentProperties(0, 0, "$(" + text + ")"));
    }

}