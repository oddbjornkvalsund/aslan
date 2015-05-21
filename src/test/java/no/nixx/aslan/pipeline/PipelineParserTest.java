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
        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command command = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("ls", command.getExecutableName());
        assertEquals(asList("foo", "bar", "arg with spaces"), command.getRenderedArguments());
    }

    @Test
    public void testParsePipeline() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("ls | grep foo");

        assertEquals(2, pipeline.getCommandsUnmodifiable().size());

        final Command lsCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("ls", lsCommand.getExecutableName());
        assertTrue(lsCommand.getRenderedArguments().isEmpty());

        final Command grepCommand = pipeline.getCommandsUnmodifiable().get(1);
        assertEquals("grep", grepCommand.getExecutableName());
        assertEquals(asList("foo"), grepCommand.getRenderedArguments());
    }

    @Test
    public void testParseVariableSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo ${HOME}");

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
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

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArguments().size());
        assertTrue(echoCommand.getArguments().get(1) instanceof CommandSubstitution);

        final CommandSubstitution cs = (CommandSubstitution) echoCommand.getArguments().get(1);
        final Pipeline csPipeline = cs.getPipeline();
        final Command csEchoCommand = csPipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", csEchoCommand.getExecutableName());
        assertEquals(asList("foo"), csEchoCommand.getRenderedArguments());
    }

    @Test
    public void testParseQuotedStringWithVariableSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo \"hello there ${FOO}\"");

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
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

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
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
        final List<Command> csCommands = cs.getPipeline().getCommandsUnmodifiable();

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
        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
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
        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command command = pipeline.getCommandsUnmodifiable().get(0);
        final List<Argument> arguments = command.getArguments();
        assertEquals(4, arguments.size());

        final Argument arg0 = arguments.get(0);
        assertTrue(arg0 instanceof Literal);
        assertEquals("echo", arg0.getRenderableText());
        assertEquals(0, arg0.startIndex);
        assertEquals(4, arg0.stopIndex);
        assertEquals("echo", arg0.unprocessedArgument);

        final Argument arg1 = arguments.get(1);
        assertTrue(arg1 instanceof Literal);
        assertEquals("first", arg1.getRenderableText());
        assertEquals(10, arg1.startIndex);
        assertEquals(15, arg1.stopIndex);
        assertEquals("first", arg1.unprocessedArgument);

        final Argument arg2 = arguments.get(2);
        assertTrue(arg2 instanceof Literal);
        assertEquals("complex  Argument  ", arg2.getRenderableText());
        assertEquals(19, arg2.startIndex);
        assertEquals(40, arg2.stopIndex);
        assertEquals("complex'  Argument  '", arg2.unprocessedArgument);

        final Argument arg3 = arguments.get(3);
        assertTrue(arg3 instanceof CommandSubstitution);
        assertFalse(arg3.isRenderableTextAvailableWithoutCommmandExecution());
        assertEquals(41, arg3.startIndex);
        assertEquals(64, arg3.stopIndex);
        assertEquals("$(cs and \"inner $(cs)\")", arg3.unprocessedArgument);

        final CommandSubstitution cs = (CommandSubstitution) arg3;
        final List<Command> csCommands = cs.getPipeline().getCommandsUnmodifiable();
        assertEquals(1, csCommands.size());
        final List<Argument> csArguments = csCommands.get(0).getArguments();

        assertTrue(csArguments.get(0) instanceof Literal);
        final Literal csArg0 = (Literal) csArguments.get(0);
        assertEquals("cs", csArg0.getRenderableText());
        assertEquals(43, csArg0.startIndex);
        assertEquals(45, csArg0.stopIndex);
        assertEquals("cs", csArg0.unprocessedArgument);

        assertTrue(csArguments.get(1) instanceof Literal);
        final Literal csArg1 = (Literal) csArguments.get(1);
        assertEquals("and", csArg1.getRenderableText());
        assertEquals(46, csArg1.startIndex);
        assertEquals(49, csArg1.stopIndex);
        assertEquals("and", csArg1.unprocessedArgument);

        assertTrue(csArguments.get(2) instanceof QuotedString);
        final QuotedString csArg2 = (QuotedString) csArguments.get(2);
        assertFalse(csArg2.isRenderableTextAvailableWithoutCommmandExecution());
        assertEquals(50, csArg2.startIndex);
        assertEquals(63, csArg2.stopIndex);
        assertEquals("\"inner $(cs)\"", csArg2.unprocessedArgument);
    }

    @Test(expected = ParseException.class)
    public void testErrorHandling() {
        final PipelineParser parser = new PipelineParser();
        parser.parseCommand("echo $(echo");
    }
}