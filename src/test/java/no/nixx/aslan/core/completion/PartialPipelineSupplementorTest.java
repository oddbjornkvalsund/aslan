package no.nixx.aslan.core.completion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PartialPipelineSupplementorTest {

    final PartialPipelineSupplementor supplementor = new PartialPipelineSupplementor();

    @Test(expected = IllegalArgumentException.class)
    public void testNullPointerExceptionOnNull() {
        supplementor.supplementPipeline(null);
    }

    @Test
    public void testEmptyReturnsEmpty() {
        assertEquals("", supplementor.supplementPipeline(""));
    }

    @Test
    public void testNoSupplementNeeded() {
        assertEquals("echo", supplementor.supplementPipeline("echo"));
    }

    @Test
    public void testSupplementForSingleQuotedString() {
        assertEquals("''", supplementor.supplementPipeline("'"));
        assertEquals("'echo'", supplementor.supplementPipeline("'echo"));
        assertEquals("'echo' 'foo'", supplementor.supplementPipeline("'echo' 'foo"));
        assertEquals("'echo' 'foo' 'bar'", supplementor.supplementPipeline("'echo' 'foo' 'bar"));
    }

    @Test
    public void testSupplementForDoubleQuotedString() {
        assertEquals("\"\"", supplementor.supplementPipeline("\""));
        assertEquals("\"echo\"", supplementor.supplementPipeline("\"echo"));
        assertEquals("\"echo\" \"foo\"", supplementor.supplementPipeline("\"echo\" \"foo"));
        assertEquals("\"echo\" \"foo\" \"bar\"", supplementor.supplementPipeline("\"echo\" \"foo\" \"bar"));
    }

    @Test
    public void testSupplementForCommandSubstitution() {
        assertEquals("$()", supplementor.supplementPipeline("$("));
        assertEquals("$(echo)", supplementor.supplementPipeline("$(echo"));
        assertEquals("$(echo)$()", supplementor.supplementPipeline("$(echo)$("));
        assertEquals(")", supplementor.supplementPipeline(")"));
        assertEquals("$())", supplementor.supplementPipeline("$())"));

        // In single quotes -> don't supplement
        assertEquals("'$('", supplementor.supplementPipeline("'$("));

        // In double quotes -> do supplement
        assertEquals("\"$()\"", supplementor.supplementPipeline("\"$("));

        // Nested single quotes in double quotes
        assertEquals("\"$('')\"", supplementor.supplementPipeline("\"$('"));

        // Nested double quotes in double quotes, not currently supported by the parser, but the supplementor handles it
        assertEquals("\"$(\"\")\"", supplementor.supplementPipeline("\"$(\""));
    }

    @Test
    public void testSupplementForVariableSubstitution() {
        assertEquals("${}", supplementor.supplementPipeline("${"));
        assertEquals("${HOME}", supplementor.supplementPipeline("${HOME"));
        assertEquals("${HOME}${}", supplementor.supplementPipeline("${HOME}${"));
        assertEquals("}", supplementor.supplementPipeline("}"));
        assertEquals("${}}", supplementor.supplementPipeline("${}}"));

        // In single quotes -> don't supplement
        assertEquals("'${'", supplementor.supplementPipeline("'${"));

        // In double quotes -> do supplement
        assertEquals("\"${}\"", supplementor.supplementPipeline("\"${"));

        // Nested single quotes in double quotes
        assertEquals("\"${''}\"", supplementor.supplementPipeline("\"${'"));

        // Nested double quotes in double quotes, not currently supported by the parser, but the supplementor handles it
        assertEquals("\"${\"\"}\"", supplementor.supplementPipeline("\"${\""));
    }

}