package no.nixx.aslan.core.completion;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.completion.specs.TestFilesCompletionSpec;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.WorkingDirectoryImpl;
import no.nixx.aslan.util.TestExecutable;
import no.nixx.aslan.util.TestExecutableLocator;
import no.nixx.aslan.util.TestExecutionContext;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
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
                    "space",
                    option("one space"),
                    option("and two spaces")

            ),
            option(
                    "remove"
            )
    );
    final ExecutableLocator executableLocator = new TestExecutableLocator("git", completionSpec);
    final ExecutionContext executionContext = new TestExecutionContext(new WorkingDirectoryImpl("."));

    @Test
    public void testCompletion() {
        CompletionResult result;

        result = completor.getCompletions("g", 1, executableLocator, executionContext);
        assertEquals(new CompletionResult("git ", 4, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git", 3, executableLocator, executionContext);
        assertEquals(new CompletionResult("git ", 4, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git ", 4, executableLocator, executionContext);
        assertEquals(new CompletionResult("git ", 4, asList("add", "space", "remove")), result);

        result = completor.getCompletions("git a", 5, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add ", 8, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git r", 5, executableLocator, executionContext);
        assertEquals(new CompletionResult("git remove ", 11, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add", 7, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add ", 8, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add ", 8, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add file ", 13, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add file f", 14, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add file file", 17, asList("fileA", "fileB", "fileC")), result);

        result = completor.getCompletions("git add file fileA", 18, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add file fileA ", 19, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git add file f --", 17, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add file f --verbos", 23, asList("--verbosity", "--verbose")), result);

        result = completor.getCompletions("git add file --verbosity ", 25, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add file --verbosity ", 25, asList("low", "high")), result);

        result = completor.getCompletions("git add file --verbosity high ", 30, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add file --verbosity high ", 30, asList("--verbose", "fileA", "fileB", "fileC")), result);
    }

    @Test
    public void testInlineCompletion() {
        CompletionResult result;

        result = completor.getCompletions("git add file f fileB", 14, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add file file fileB", 17, asList("fileA", "fileB", "fileC")), result);

        result = completor.getCompletions("foo", 0, executableLocator, executionContext);
        assertEquals(new CompletionResult("git ", 4, asList()), result);

        result = completor.getCompletions(" foo", 0, executableLocator, executionContext);
        assertEquals(new CompletionResult("git  foo", 4, asList()), result);

        result = completor.getCompletions("grit", 1, executableLocator, executionContext);
        assertEquals(new CompletionResult("git ", 4, asList()), result);

        result = completor.getCompletions("git asdd", 5, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add ", 8, asList()), result);

        result = completor.getCompletions("git  ", 4, executableLocator, executionContext);
        assertEquals(new CompletionResult("git  ", 4, asList("add", "space", "remove")), result);
    }

    @Test
    public void testCompletionInPipeline() {
        CompletionResult result;

        result = completor.getCompletions("git | git", 9, executableLocator, executionContext);
        assertEquals(new CompletionResult("git | git ", 10, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git a | git", 5, executableLocator, executionContext);
        assertEquals(new CompletionResult("git add  | git", 8, Collections.<String>emptyList()), result);

        result = completor.getCompletions("echo foo | git add f", 20, executableLocator, executionContext);
        assertEquals(new CompletionResult("echo foo | git add file ", 24, Collections.<String>emptyList()), result);
    }

    @Test
    public void testCompletionInCommandSubstitution() {
        CompletionResult result;

        result = completor.getCompletions("echo $(git add f", 16, executableLocator, executionContext);
        assertEquals(new CompletionResult("echo $(git add file ", 20, Collections.<String>emptyList()), result);

        result = completor.getCompletions("echo $(echo $(echo | git add f", 30, executableLocator, executionContext);
        assertEquals(new CompletionResult("echo $(echo $(echo | git add file ", 34, Collections.<String>emptyList()), result);
    }

    @Test
    public void testCompletionWithSpaces() {
        CompletionResult result;

        result = completor.getCompletions("git space one", 13, executableLocator, executionContext);
        assertEquals(new CompletionResult("git space \"one space\" ", 22, Collections.<String>emptyList()), result);

        result = completor.getCompletions("git space ", 10, executableLocator, executionContext);
        assertEquals(new CompletionResult("git space \"", 11, asList("\"one space\"", "\"and two spaces\"")), result);

        result = completor.getCompletions("git space \"", 11, executableLocator, executionContext);
        assertEquals(new CompletionResult("git space \"", 11, asList("\"one space\"", "\"and two spaces\"")), result);

        result = completor.getCompletions("git space \"one space\"", 21, executableLocator, executionContext);
        assertEquals(new CompletionResult("git space \"one space\" ", 22, Collections.<String>emptyList()), result);
    }

    @Test
    public void testCompletionWithSpacesAndAppendingOfSpaceAndQuotes() {
        final CompletionSpecRoot root = new CompletionSpecRoot(
                new CompletionSpec() {

                    final String completion = "dir with spaces\\";

                    @Override
                    public boolean isPartialMatch(String argument) {
                        return completion.startsWith(argument) && !isCompleteMatch(argument);
                    }

                    @Override
                    public boolean isCompleteMatch(String argument) {
                        return completion.equals(argument);
                    }

                    @Override
                    public List<String> getCompletions(String argument) {
                        return asList(completion);
                    }

                    @Override
                    public boolean appendSpaceIfOnlyOneCompletion() {
                        return false;
                    }

                    @Override
                    public boolean appendQuoteIfOnlyOneCompletion() {
                        return false;
                    }
                },
                new CompletionSpec() {

                    final String completion = "file with spaces";

                    @Override
                    public boolean isPartialMatch(String argument) {
                        return completion.startsWith(argument) && !isCompleteMatch(argument);
                    }

                    @Override
                    public boolean isCompleteMatch(String argument) {
                        return completion.equals(argument);
                    }

                    @Override
                    public List<String> getCompletions(String argument) {
                        return asList(completion);
                    }

                    @Override
                    public boolean appendSpaceIfOnlyOneCompletion() {
                        return true;
                    }
                }
        );

        final ExecutableLocator executableLocator = new TestExecutableLocator("foo", root);

        CompletionResult result;
        result = completor.getCompletions("foo d", 5, executableLocator, executionContext);
        assertEquals(new CompletionResult("foo \"dir with spaces\\", 21, Collections.<String>emptyList()), result);

        result = completor.getCompletions("foo \"d", 6, executableLocator, executionContext);
        assertEquals(new CompletionResult("foo \"dir with spaces\\", 21, Collections.<String>emptyList()), result);

        result = completor.getCompletions("foo f", 5, executableLocator, executionContext);
        assertEquals(new CompletionResult("foo \"file with spaces\" ", 23, Collections.<String>emptyList()), result);

        result = completor.getCompletions("foo \"f", 6, executableLocator, executionContext);
        assertEquals(new CompletionResult("foo \"file with spaces\" ", 23, Collections.<String>emptyList()), result);
    }

    @Test
    public void testManyMatchingExecutablesNames() {
        final CompletionSpecRoot completionSpecRoot = new CompletionSpecRoot();
        final ExecutableLocator executableLocator = new ExecutableLocator() {
            final List<String> executableNames = asList("foo", "foobar");

            @Override
            public Executable lookupExecutable(String name) {
                return new TestExecutable(completionSpecRoot);
            }

            @Override
            public List<String> findExecutableCandidates(String name) {
                return executableNames.stream().filter(ec -> ec.startsWith(name)).collect(toList());
            }
        };

        final CompletionResult result = completor.getCompletions("foo", 3, executableLocator, executionContext);
        assertEquals(new CompletionResult("foo", 3, asList("foo", "foobar")), result);
    }

    private CompletionSpec files() {
        return new TestFilesCompletionSpec("fileA", "fileB", "fileC");
    }
}