package no.nixx.aslan.completion;

import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.Completor;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static no.nixx.aslan.core.completion.CompletionSpec.*;
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

    @Test
    public void testCompletion() {
        String command = "git'";
        assertEquals(Collections.<String>emptyList(), getCompletions(command));

        command = "git '";
        assertEquals(asList("add", "remove"), getCompletions(command));

        command = "git add '";
        assertEquals(asList("file"), getCompletions(command));

        command = "git add file '";
        assertEquals(asList("--verbosity", "--verbose", "fileA", "fileB", "fileC"), getCompletions(command));

        command = "git add file file'";
        assertEquals(asList("fileA", "fileB", "fileC"), getCompletions(command));

        command = "git add file fileA file'";
        assertEquals(asList("fileA", "fileB", "fileC"), getCompletions(command));

        command = "git add file fileA --'";
        assertEquals(asList("--verbosity", "--verbose"), getCompletions(command));

        command = "git add file fileA --verbose '";
        assertEquals(asList("--verbosity", "fileA", "fileB", "fileC"), getCompletions(command));

        command = "git add file fileA --verbosity '";
        assertEquals(asList("low", "high"), getCompletions(command));

        command = "git add file fileA --verbosity low '";
        assertEquals(asList("--verbose", "fileA", "fileB", "fileC"), getCompletions(command));

        command = "git add file fileA --verbosity low --verbose '";
        assertEquals(asList("fileA", "fileB", "fileC"), getCompletions(command));
    }

    private List<String> getCompletions(String command) {
        return completor.getCompletions(command, getTabPosition(command), completionSpec);
    }

    private int getTabPosition(String command) {
        return command.indexOf("'");
    }
}