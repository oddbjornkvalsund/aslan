package no.nixx.aslan.pipeline;

import no.nixx.aslan.pipeline.model.*;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PipelineParserTest {

    @Test
    public void testParseSingleCommand() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("ls foo bar 'arg with spaces'");
        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command command = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("ls", command.getExecutableName());
        assertEquals(asList("foo", "bar", "arg with spaces"), command.getArgumentsAsStrings());
    }

    @Test
    public void testParsePipeline() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("ls | grep foo");

        assertEquals(2, pipeline.getCommandsUnmodifiable().size());

        final Command lsCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("ls", lsCommand.getExecutableName());
        assertTrue(lsCommand.getArgumentsAsStrings().isEmpty());

        final Command grepCommand = pipeline.getCommandsUnmodifiable().get(1);
        assertEquals("grep", grepCommand.getExecutableName());
        assertEquals(asList("foo"), grepCommand.getArgumentsAsStrings());
    }

    @Test
    public void testParseVariableSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo ${HOME}");

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(asList(new Literal("echo"), new VariableSubstitution("HOME")), echoCommand.getArgumentsUnmodifiable());
    }

    @Test
    public void testParseCommandSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo $(echo foo)");

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArgumentsUnmodifiable().size());
        assertTrue(echoCommand.getArgumentsUnmodifiable().get(1) instanceof CommandSubstitution);

        final CommandSubstitution cs = (CommandSubstitution) echoCommand.getArgumentsUnmodifiable().get(1);
        final Pipeline csPipeline = cs.getPipeline();
        final Command csEchoCommand = csPipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", csEchoCommand.getExecutableName());
        assertEquals(asList("foo"), csEchoCommand.getArgumentsAsStrings());
    }

    @Test
    public void testParseQuotedStringWithVariableSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo \"hello there ${FOO}\"");

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArgumentsUnmodifiable().size());

        final Argument argument = echoCommand.getArgumentsUnmodifiable().get(1);
        assertTrue(argument instanceof QuotedString);

        final QuotedString quotedString = (QuotedString) argument;
        assertEquals("hello there ", quotedString.getText());

        final List<QuotedString.Component> components = quotedString.getComponentsUnmodifiable();
        assertEquals(1, components.size());
        final QuotedString.Component component = components.get(0);
        assertEquals(12, component.position);
        assertEquals(new VariableSubstitution("FOO"), component.argument);
    }

    @Test
    public void testParseQuotedStringWithCommandSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo \"hello there $(echo foo)\"");

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArgumentsUnmodifiable().size());

        final Argument argument = echoCommand.getArgumentsUnmodifiable().get(1);
        assertTrue(argument instanceof QuotedString);

        final QuotedString quotedString = (QuotedString) argument;
        assertEquals("hello there ", quotedString.getText());

        final List<QuotedString.Component> components = quotedString.getComponentsUnmodifiable();
        assertEquals(1, components.size());
        final QuotedString.Component component = components.get(0);
        assertEquals(12, component.position);
        assertTrue(component.argument instanceof CommandSubstitution);

        final CommandSubstitution cs = (CommandSubstitution) component.argument;
        final List<Command> csCommands = cs.getPipeline().getCommandsUnmodifiable();

        assertEquals(1, csCommands.size());

        final Command csEchoCommand = csCommands.get(0);
        assertEquals(2, csEchoCommand.getArgumentsUnmodifiable().size());
        assertEquals("echo", csEchoCommand.getExecutableName());
        assertEquals(asList("foo"), csEchoCommand.getArgumentsAsStrings());
    }

    @Test
    public void testParseCompositeArgument() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo complex'single quoted'\"double quoted\"$(cs with'nested'\"complex\")${VS}");
        assertEquals(1, pipeline.getCommandsUnmodifiable().size());

        final Command echoCommand = pipeline.getCommandsUnmodifiable().get(0);
        assertEquals("echo", echoCommand.getExecutableName());
        assertEquals(2, echoCommand.getArgumentsUnmodifiable().size());

        final Argument argument = echoCommand.getArgumentsUnmodifiable().get(1);
        assertTrue(argument.isCompositeArgument());
        final CompositeArgument compositeArgument = (CompositeArgument) argument;
        assertEquals(compositeArgument.size(), 5);
        assertEquals(new Literal("complex"), compositeArgument.get(0));
        assertEquals(new Literal("single quoted"), compositeArgument.get(1));
        assertEquals(new QuotedString("double quoted", Collections.<QuotedString.Component>emptyList()), compositeArgument.get(2));
        assertTrue(compositeArgument.get(3).isCommandSubstitution());
        assertTrue(compositeArgument.get(4).isVariableSubstitution());
    }

    @Test(expected = ParseException.class)
    public void testErrorHandling() {
        final PipelineParser parser = new PipelineParser();
        parser.parseCommand("echo $(echo");
    }
}