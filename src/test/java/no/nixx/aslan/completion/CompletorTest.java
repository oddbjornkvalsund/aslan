package no.nixx.aslan.completion;

import no.nixx.aslan.completion.specs.TestFilesCompletionSpec;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.completion.CompletionResult;
import no.nixx.aslan.core.completion.CompletionSpec;
import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.Completor;
import no.nixx.aslan.util.TestExecutableLocator;
import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static no.nixx.aslan.core.completion.CompletionSpec.keywords;
import static no.nixx.aslan.core.completion.CompletionSpec.option;
import static org.junit.Assert.assertEquals;

public class CompletorTest {

    final Completor completor = new Completor();
    final CompletionSpecRoot completionSpec = new CompletionSpecRoot(
            option(
                    "add",
                    option(
                            "file",
                            option("--verbosity", keywords("low", "high")),
                            option("--verbose"),
                            files()
                    )
            ),
            option(
                    "remove"
            )
    );
    final ExecutableLocator executableLocator = new TestExecutableLocator("git", completionSpec);

    @Test
    public void testCompletion() {
        CompletionResult result;

        result = completor.getCompletions("g", 1, executableLocator, null);
        assertEquals(new CompletionResult(4, "git ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git", 3, executableLocator, null);
        assertEquals(new CompletionResult(4, "git ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git ", 4, executableLocator, null);
        assertEquals(new CompletionResult(4, "git ", asList("add", "remove")), result);

        result = completor.getCompletions("git a", 5, executableLocator, null);
        assertEquals(new CompletionResult(8, "git add ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git r", 5, executableLocator, null);
        assertEquals(new CompletionResult(11, "git remove ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add ", 8, executableLocator, null);
        assertEquals(new CompletionResult(13, "git add file ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add file f", 14, executableLocator, null);
        assertEquals(new CompletionResult(17, "git add file file", asList("fileA", "fileB", "fileC")), result);

        result = completor.getCompletions("git add file fileA", 18, executableLocator, null);
        assertEquals(new CompletionResult(19, "git add file fileA ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add file f --", 17, executableLocator, null);
        assertEquals(new CompletionResult(23, "git add file f --verbos", asList("--verbosity", "--verbose")), result);

        result = completor.getCompletions("git add file --verbosity ", 25, executableLocator, null);
        assertEquals(new CompletionResult(25, "git add file --verbosity ", asList("low", "high")), result);

        result = completor.getCompletions("git add file --verbosity high ", 30, executableLocator, null);
        assertEquals(new CompletionResult(30, "git add file --verbosity high ", asList("--verbose", "fileA", "fileB", "fileC")), result);
    }

    @Test
    public void testInlineCompletion() {
        CompletionResult result;

        result = completor.getCompletions("git add file f fileB", 14, executableLocator, null);
        assertEquals(new CompletionResult(17, "git add file file fileB", asList("fileA", "fileB", "fileC")), result);

    }

    private CompletionSpec files() {
        return new TestFilesCompletionSpec("fileA", "fileB", "fileC");
    }
}