package no.nixx.aslan.pipeline.model;

import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArgumentTest {

    @Test
    public void testSpansPosition() {
        final Literal argument = new Literal("foo", new ArgumentProperties(0, 3, "foo"));
        assertTrue(argument.spansPosition(0));
        assertTrue(argument.spansPosition(3));

        final Literal emptyArgument = new Literal("", new ArgumentProperties(0, 0, ""));
        assertTrue(emptyArgument.spansPosition(0));
    }

    @Test
    public void testSubstring() {
        final Literal argument = new Literal("foo", new ArgumentProperties(0, 3, "foo"));
        assertEquals("", argument.substring(0));
        assertEquals("f", argument.substring(1));
        assertEquals("fo", argument.substring(2));
        assertEquals("foo", argument.substring(3));

        final Literal argument1 = new Literal("foo", new ArgumentProperties(100, 103, "foo"));
        assertEquals("", argument1.substring(100));
        assertEquals("f", argument1.substring(101));
        assertEquals("fo", argument1.substring(102));
        assertEquals("foo", argument1.substring(103));

        final Literal argument2 = new Literal("", new ArgumentProperties(0, 2, "\"\""));
        assertEquals("", argument2.substring(2));

        final Literal argument3 = new Literal("''", new ArgumentProperties(0, 4, "\"''\""));
        assertEquals("", argument3.substring(0));
        assertEquals("", argument3.substring(1));
        assertEquals("'", argument3.substring(2));
        assertEquals("''", argument3.substring(3));
        assertEquals("''", argument3.substring(4));

        final Literal argument4 = new Literal("\"\"", new ArgumentProperties(0, 4, "'\"\"'"));
        assertEquals("", argument4.substring(0));
        assertEquals("", argument4.substring(1));
        assertEquals("\"", argument4.substring(2));
        assertEquals("\"\"", argument4.substring(3));
        assertEquals("\"\"", argument4.substring(4));

        final Literal argument5 = new Literal("", new ArgumentProperties(0, 4, "\"\"''"));
        assertEquals("", argument5.substring(0));
        assertEquals("", argument5.substring(1));
        assertEquals("", argument5.substring(2));
        assertEquals("", argument5.substring(3));
        assertEquals("", argument5.substring(4));
        assertThrowsException(IllegalArgumentException.class, argument5, arg -> arg.substring(-1));
        assertThrowsException(IllegalArgumentException.class, argument5, arg -> arg.substring(5));
    }

    private <T> void assertThrowsException(Class<? extends Exception> expectedException, T object, Function<T, ?> function) {
        try {
            function.apply(object);
            fail("Expected exception: " + expectedException);
        } catch (Exception exception) {
            if (!exception.getClass().equals(expectedException)) {
                throw exception;
            }
        }
    }
}