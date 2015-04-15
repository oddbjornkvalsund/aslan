package no.nixx.aslan.core;

import no.nixx.aslan.core.completion.PartialCommandExtractor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PartialCommandExtractorTest {

    @Test
    public void testGetLastCommand() {
        final PartialCommandExtractor ce = new PartialCommandExtractor();

        // Pipelines
        assertEquals(ce.getLastCommand(""), "");
        assertEquals(ce.getLastCommand("|"), "");
        assertEquals(ce.getLastCommand("||||"), "");
        assertEquals(ce.getLastCommand("echo foo"), "echo foo");
        assertEquals(ce.getLastCommand("echo foo | grep foo"), " grep foo");
        assertEquals(ce.getLastCommand("echo foo | grep foo | quux"), " quux");

        // Pipelines in single quotes
        assertEquals(ce.getLastCommand("echo foo '| grep foo' '| quux'"), "echo foo '| grep foo' '| quux'");
        assertEquals(ce.getLastCommand("echo foo '| grep foo' '| quux"), "echo foo '| grep foo' '| quux");

        // Closed command substitution
        assertEquals(ce.getLastCommand("foo $(bar)"), "foo $(bar)");
        assertEquals(ce.getLastCommand("foo $(bar $(quux))"), "foo $(bar $(quux))");

        // Open command substitution
        assertEquals(ce.getLastCommand("foo $(bar"), "bar");
        assertEquals(ce.getLastCommand("foo $(bar $(quux"), "quux");

        // Open command substitution in single quotes
        assertEquals(ce.getLastCommand("foo '$(  bar  "), "foo '$(  bar  ");
        assertEquals(ce.getLastCommand("foo '$(  bar $(  quux  "), "foo '$(  bar $(  quux  ");

        // Open command substitution in double quotes
        assertEquals(ce.getLastCommand("foo \"$(  bar  "), "  bar  ");
        assertEquals(ce.getLastCommand("foo \"$(  bar $(  quux  "), "  quux  ");
    }
}