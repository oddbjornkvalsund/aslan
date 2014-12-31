package no.nixx.wing.pipeline;

import org.junit.Ignore;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

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

    @Ignore // TODO!
    @Test
    public void testParseCommandSubstitution() {
        final PipelineParser parser = new PipelineParser();
        final Pipeline pipeline = parser.parseCommand("echo $(echo foo)");

        assertEquals(1, pipeline.getCommandsUnmodifiable().size());
    }
}