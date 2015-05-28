package no.nixx.aslan.pipeline.model;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CommandTest {
    @Test
    public void testIsFirstArgument() {
        final Literal fooArgument = new Literal("foo", new ArgumentProperties(0, 3, "foo"));
        final Command fooCommand = new Command(new CommandProperties(0, 3), fooArgument);
        assertTrue(fooCommand.isFirstArgument(fooArgument));

        final Literal barArgument = new Literal("bar", new ArgumentProperties(1, 4, "foo"));
        final Command barCommand = new Command(new CommandProperties(0, 4), barArgument);
        assertTrue(barCommand.isFirstArgument(barArgument));

        final Literal emptyArgument = new Literal("", new ArgumentProperties(0, 0, ""));
        assertTrue(barCommand.isFirstArgument(emptyArgument));
    }
}