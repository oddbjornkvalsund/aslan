package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.core.WorkingDirectoryImpl;
import no.nixx.aslan.core.completion.CompletionResult;
import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.Completor;
import no.nixx.aslan.util.TestExecutableLocator;
import no.nixx.aslan.util.TestExecutionContext;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeywordCompletionSpecTest {

    @Test(expected = NullPointerException.class)
    public void testNull() {
        new KeywordCompletionSpec((String)null);
    }

    @Test(expected = NullPointerException.class)
    public void testIsPartialMathOfNull() {
        final KeywordCompletionSpec spec = new KeywordCompletionSpec("foo");
        spec.isPartialMatch(null);
    }

    @Test(expected = NullPointerException.class)
    public void testIsCompleteMatchOfNull() {
        final KeywordCompletionSpec spec = new KeywordCompletionSpec("foo");
        spec.isCompleteMatch(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCompletionsOfNull() {
        final KeywordCompletionSpec spec = new KeywordCompletionSpec("foo");
        spec.getCompletions(null);
    }

    @Test
    public void testEmpty() {
        final KeywordCompletionSpec spec = new KeywordCompletionSpec();
        assertTrue(spec.isPartialMatch(""));
        assertFalse(spec.isCompleteMatch(""));
        assertTrue(spec.getCompletions("").isEmpty());
    }

    @Test
    public void testSimpleCases() {
        final KeywordCompletionSpec spec = new KeywordCompletionSpec("first", "second");

        assertTrue(spec.isPartialMatch("f"));
        assertFalse(spec.isPartialMatch("first"));

        assertFalse(spec.isCompleteMatch("f"));
        assertTrue(spec.isCompleteMatch("first"));

        assertTrue(spec.isPartialMatch("s"));
        assertFalse(spec.isPartialMatch("second"));

        assertFalse(spec.isCompleteMatch("s"));
        assertTrue(spec.isCompleteMatch("second"));

        assertEquals(asList("first"), spec.getCompletions("f"));
        assertEquals(asList("first"), spec.getCompletions("first"));
        assertEquals(asList("second"), spec.getCompletions("s"));
        assertEquals(asList("second"), spec.getCompletions("second"));
        assertEquals(asList("first", "second"), spec.getCompletions(""));
    }

    @Test
    public void test() {
        final CompletionSpecRoot root = new CompletionSpecRoot(new KeywordCompletionSpec("first", "second"));
        final TestExecutableLocator el = new TestExecutableLocator("cmd", root);
        final TestExecutionContext ec = new TestExecutionContext(new WorkingDirectoryImpl("/"));

        final Completor completor = new Completor();
        final List<String> noCompletionCandidates = Collections.<String>emptyList();

        assertEquals(new CompletionResult("cmd ", 4, asList("first", "second")), completor.getCompletions("cmd ", 4, el, ec));
        assertEquals(new CompletionResult("cmd first ", 10, noCompletionCandidates), completor.getCompletions("cmd f", 5, el, ec));
        assertEquals(new CompletionResult("cmd first ", 10, noCompletionCandidates), completor.getCompletions("cmd first", 9, el, ec));
        assertEquals(new CompletionResult("cmd second ", 11, noCompletionCandidates), completor.getCompletions("cmd s", 5, el, ec));
        assertEquals(new CompletionResult("cmd second ", 11, noCompletionCandidates), completor.getCompletions("cmd second", 10, el, ec));

        // Mutually exclusive
        assertEquals(new CompletionResult("cmd first ", 10, noCompletionCandidates), completor.getCompletions("cmd first ", 10, el, ec));
        assertEquals(new CompletionResult("cmd second ", 11, noCompletionCandidates), completor.getCompletions("cmd second ", 11, el, ec));
    }
}