package no.nixx.aslan.pipeline.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PipelineTest {

    @Test
    public void testGetCommandAtPosition() {
        // "foo|bar"
        final Command fooCommand = new Command(new CommandProperties(0, 3), new Literal("foo", new ArgumentProperties(0, 3, "foo")));
        final Command barCommand = new Command(new CommandProperties(4, 7), new Literal("bar", new ArgumentProperties(4, 7, "bar")));
        final Pipeline pipeline = new Pipeline(fooCommand, barCommand);
        assertEquals(fooCommand, pipeline.getCommandAtPosition(0));
        assertEquals(fooCommand, pipeline.getCommandAtPosition(3));
        assertEquals(barCommand, pipeline.getCommandAtPosition(4));
        assertEquals(barCommand, pipeline.getCommandAtPosition(7));
    }

    @Test
    public void testGetCommandAtPositionWithSpaces() {
        // " foo | bar "
        final Command fooCommand = new Command(new CommandProperties(0, 5), new Literal("foo", new ArgumentProperties(1, 4, "foo")));
        final Command barCommand = new Command(new CommandProperties(0, 11), new Literal("bar", new ArgumentProperties(7, 10, "bar")));
        final Pipeline pipeline = new Pipeline(fooCommand, barCommand);

        assertEquals(fooCommand, pipeline.getCommandAtPosition(0));
        assertEquals(fooCommand, pipeline.getCommandAtPosition(4));
        assertEquals(barCommand, pipeline.getCommandAtPosition(6));
        assertEquals(barCommand, pipeline.getCommandAtPosition(11));
    }
}