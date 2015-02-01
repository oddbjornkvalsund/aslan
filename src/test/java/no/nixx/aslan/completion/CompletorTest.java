package no.nixx.aslan.completion;

import no.nixx.aslan.completion.specs.TestFilesCompletionSpec;
import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.ExecutionContext;
import no.nixx.aslan.core.completion.*;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

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
    @SuppressWarnings("Convert2Lambda")
    final ExecutableLocator executableLocator = new ExecutableLocator() {

        @Override
        public Executable lookupExecutable(String name) {
            return new TestExecutable();
        }

        class TestExecutable implements Executable, Completable {

            @Override
            public CompletionSpecRoot getCompletionSpec() {
                return completionSpec;
            }

            @Override
            public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
            }

            @Override
            public void run() {
            }

            @Override
            public int getExitStatus() {
                return 0;
            }
        }
    };

    @Test
    public void testResult() {
        CompletionResult result;

        // TODO: Support this
//        result = completor.getCompletions("g", 1, executableLocator);
//        assertEquals(new CompletionResult(4, "git ", Collections.<String>emptyList()), result);

        // TODO: Support this
//        result = completor.getCompletions("git", 3, executableLocator);
//        assertEquals(new CompletionResult(4, "git ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git ", 4, executableLocator);
        assertEquals(new CompletionResult(4, "git ", asList("add", "remove")), result);

        result = completor.getCompletions("git a", 5, executableLocator);
        assertEquals(new CompletionResult(8, "git add ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git r", 5, executableLocator);
        assertEquals(new CompletionResult(11, "git remove ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add ", 8, executableLocator);
        assertEquals(new CompletionResult(13, "git add file ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add file f", 14, executableLocator);
        assertEquals(new CompletionResult(17, "git add file file", asList("fileA", "fileB", "fileC")), result);

        result = completor.getCompletions("git add file fileA", 18, executableLocator);
        assertEquals(new CompletionResult(19, "git add file fileA ", Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add file f --", 17, executableLocator);
        assertEquals(new CompletionResult(23, "git add file f --verbos", asList("--verbosity", "--verbose")), result);

        result = completor.getCompletions("git add file --verbosity ", 25, executableLocator);
        assertEquals(new CompletionResult(25, "git add file --verbosity ", asList("low", "high")), result);

        result = completor.getCompletions("git add file --verbosity high ", 30, executableLocator);
        assertEquals(new CompletionResult(30, "git add file --verbosity high ", asList("--verbose", "fileA", "fileB", "fileC")), result);
    }

    private CompletionSpec files() {
        return new TestFilesCompletionSpec("fileA", "fileB", "fileC");
    }
}