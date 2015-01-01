package no.nixx.wing.pipeline;

import org.junit.Test;

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
        assertEquals(asList(), lsCommand.getArgumentsAsStrings());

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

    @Test(expected = ParseException.class)
    public void testErrorHandling() {
        final PipelineParser parser = new PipelineParser();
        parser.parseCommand("echo $(echo");
    }
}