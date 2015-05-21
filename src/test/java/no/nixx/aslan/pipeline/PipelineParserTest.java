package no.nixx.aslan.pipeline;

import no.nixx.aslan.pipeline.model.*;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PipelineParserTest {

    @Test
    public void testParseSingleCommand() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("ls foo bar 'arg with spaces'");
        assertEquals(1, pipeline.getCommands().size());

        final Command command = pipeline.getCommands().get(0);
        assertEquals("ls", command.getExecutableName());
        assertEquals(asList("foo", "bar", "arg with spaces"), command.getRenderedArguments());
    }

    @Test
    public void testParsePipeline() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("ls | grep foo");

        assertEquals(2, pipeline.getCommands().size());

        final Command lsCommand = pipeline.getCommands().get(0);
        assertEquals("ls", lsCommand.getExecutableName());
        assertTrue(lsCommand.getRenderedArguments().isEmpty());

        final Command grepCommand = pipeline.getCommands().get(1);
        assertEquals("grep", grepCommand.getExecutableName());
        assertEquals(asList("foo"), grepCommand.getRenderedArguments());
    }

    @Test
    public void testParseVariableSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo ${HOME}");

        assertEquals(1, pipeline.getCommands().size());

        final Command echoCommand = pipeline.getCommands().get(0);
        assertEquals("echo", echoCommand.getExecutableName());

        final List<Argument> arguments = echoCommand.getArguments();

        assertTrue(arguments.get(0).isLiteral());
        assertEquals("echo", ((Literal) arguments.get(0)).text);

        assertTrue(arguments.get(1).isVariableSubstitution());
        assertEquals("HOME", ((VariableSubstitution)arguments.get(1)).variableName);
    }

    @Test
    public void testParseCommandSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo $(echo foo)");

        assertEquals(1, pipeline.getCommands().size());

        final Command echoCommand = pipeline.getCommands().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArguments().size());
        assertTrue(echoCommand.getArguments().get(1) instanceof CommandSubstitution);

        final CommandSubstitution cs = (CommandSubstitution) echoCommand.getArguments().get(1);
        final Pipeline csPipeline = cs.getPipeline();
        final Command csEchoCommand = csPipeline.getCommands().get(0);
        assertEquals("echo", csEchoCommand.getExecutableName());
        assertEquals(asList("foo"), csEchoCommand.getRenderedArguments());
    }

    @Test
    public void testParseQuotedStringWithVariableSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo \"hello there ${FOO}\"");

        assertEquals(1, pipeline.getCommands().size());

        final Command echoCommand = pipeline.getCommands().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArguments().size());

        final Argument argument = echoCommand.getArguments().get(1);
        assertTrue(argument instanceof QuotedString);

        final QuotedString quotedString = (QuotedString) argument;
        assertEquals("hello there ", quotedString.getText());

        final List<QuotedString.Component> components = quotedString.getComponents();
        assertEquals(1, components.size());
        final QuotedString.Component component = components.get(0);
        assertEquals(12, component.position);
        assertTrue(component.argument.isVariableSubstitution());
        assertEquals("FOO", ((VariableSubstitution)component.argument).variableName);
    }

    @Test
    public void testParseQuotedStringWithCommandSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo \"hello there $(echo foo)\"");

        assertEquals(1, pipeline.getCommands().size());

        final Command echoCommand = pipeline.getCommands().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArguments().size());

        final Argument argument = echoCommand.getArguments().get(1);
        assertTrue(argument instanceof QuotedString);

        final QuotedString quotedString = (QuotedString) argument;
        assertEquals("hello there ", quotedString.getText());

        final List<QuotedString.Component> components = quotedString.getComponents();
        assertEquals(1, components.size());
        final QuotedString.Component component = components.get(0);
        assertEquals(12, component.position);
        assertTrue(component.argument instanceof CommandSubstitution);

        final CommandSubstitution cs = (CommandSubstitution) component.argument;
        final List<Command> csCommands = cs.getPipeline().getCommands();

        assertEquals(1, csCommands.size());

        final Command csEchoCommand = csCommands.get(0);
        assertEquals(2, csEchoCommand.getArguments().size());
        assertEquals("echo", csEchoCommand.getExecutableName());
        assertEquals(asList("foo"), csEchoCommand.getRenderedArguments());
    }

    @Test
    public void testParseCompositeArgument() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo complex'single quoted'\"double quoted\"$(cs with'nested'\"complex\")${VS}");
        assertEquals(1, pipeline.getCommands().size());

        final Command echoCommand = pipeline.getCommands().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArguments().size());

        final Argument argument = echoCommand.getArguments().get(1);
        assertTrue(argument.isCompositeArgument());
        final CompositeArgument compositeArgument = (CompositeArgument) argument;
        assertEquals(compositeArgument.size(), 5);
        assertTrue(compositeArgument.get(0).isLiteral());
        assertEquals("complex", compositeArgument.get(0).getRenderedText());
        assertTrue(compositeArgument.get(1).isLiteral());
        assertEquals("single quoted", compositeArgument.get(1).getRenderedText());
        assertTrue(compositeArgument.get(2).isQuotedString());
        final QuotedString quotedString = (QuotedString) compositeArgument.get(2);
        assertEquals("double quoted", quotedString.getText());
        assertEquals(Collections.<QuotedString.Component>emptyList(), quotedString.getComponents());
        assertTrue(compositeArgument.get(3).isCommandSubstitution());
        assertTrue(compositeArgument.get(4).isVariableSubstitution());
    }

    // TODO: Since Arguments now require startIndex/stopIndex it's hard to assert equality by "new Literal(...)"
    // TODO: Make assertLiteral(...), assertQuotedString(...) etc.

    @Test
    public void testParseAndAddArgumentPositions() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo      first    complex'  Argument  ' $(cs and \"inner $(cs)\")");
        assertEquals(1, pipeline.getCommands().size());

        final Command command = pipeline.getCommands().get(0);
        final List<Argument> arguments = command.getArguments();
        assertEquals(4, arguments.size());

        final Argument arg0 = arguments.get(0);
        assertTrue(arg0 instanceof Literal);
        assertEquals("echo", arg0.getRenderedText());
        assertEquals(0, arg0.getStartIndex());
        assertEquals(4, arg0.getStopIndex());
        assertEquals("echo", arg0.getUnprocessedArgument());

        final Argument arg1 = arguments.get(1);
        assertTrue(arg1 instanceof Literal);
        assertEquals("first", arg1.getRenderedText());
        assertEquals(10, arg1.getStartIndex());
        assertEquals(15, arg1.getStopIndex());
        assertEquals("first", arg1.getUnprocessedArgument());

        final Argument arg2 = arguments.get(2);
        assertTrue(arg2 instanceof Literal);
        assertEquals("complex  Argument  ", arg2.getRenderedText());
        assertEquals(19, arg2.getStartIndex());
        assertEquals(40, arg2.getStopIndex());
        assertEquals("complex'  Argument  '", arg2.getUnprocessedArgument());

        final Argument arg3 = arguments.get(3);
        assertTrue(arg3 instanceof CommandSubstitution);
        assertFalse(arg3.isRenderable());
        assertEquals(41, arg3.getStartIndex());
        assertEquals(64, arg3.getStopIndex());
        assertEquals("$(cs and \"inner $(cs)\")", arg3.getUnprocessedArgument());

        final CommandSubstitution cs = (CommandSubstitution) arg3;
        final List<Command> csCommands = cs.getPipeline().getCommands();
        assertEquals(1, csCommands.size());
        final List<Argument> csArguments = csCommands.get(0).getArguments();

        assertTrue(csArguments.get(0) instanceof Literal);
        final Literal csArg0 = (Literal) csArguments.get(0);
        assertEquals("cs", csArg0.getRenderedText());
        assertEquals(43, csArg0.getStartIndex());
        assertEquals(45, csArg0.getStopIndex());
        assertEquals("cs", csArg0.getUnprocessedArgument());

        assertTrue(csArguments.get(1) instanceof Literal);
        final Literal csArg1 = (Literal) csArguments.get(1);
        assertEquals("and", csArg1.getRenderedText());
        assertEquals(46, csArg1.getStartIndex());
        assertEquals(49, csArg1.getStopIndex());
        assertEquals("and", csArg1.getUnprocessedArgument());

        assertTrue(csArguments.get(2) instanceof QuotedString);
        final QuotedString csArg2 = (QuotedString) csArguments.get(2);
        assertFalse(csArg2.isRenderable());
        assertEquals(50, csArg2.getStartIndex());
        assertEquals(63, csArg2.getStopIndex());
        assertEquals("\"inner $(cs)\"", csArg2.getUnprocessedArgument());
    }

    @Test(expected = ParseException.class)
    public void testErrorHandling() {
        final PipelineParser parser = new PipelineParser();
        parser.parseCommand("echo $(echo");
    }
}