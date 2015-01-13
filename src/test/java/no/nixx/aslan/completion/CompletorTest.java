package no.nixx.aslan.completion;

import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.Completor;
import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static no.nixx.aslan.core.completion.CompletionSpec.ArgumentRequirement.REQUIRED;
import static no.nixx.aslan.core.completion.CompletionSpec.keywords;
import static no.nixx.aslan.core.completion.CompletionSpec.option;
import static org.junit.Assert.assertEquals;

public class CompletorTest {

    final Completor completor = new Completor();

    // Order matters

    private final CompletionSpecRoot completionSpecRoot = new CompletionSpecRoot(
            // Option taking no arguments: foo --verbose
            option(
                    "--verbose"
            ),

            // Option with argument: foo --sort size /usr/local/bin
            option(
                    "--sort",
                    REQUIRED,
                    keywords("name", "ntestn", "size", "ctime", "atime", "version", "extension")
            ),

            // Nested structure: sys add user matthew
            option(
                    "add",
                    REQUIRED,
                    option(
                            "user",
                            REQUIRED,
                            keywords("jim", "bob")
                    ),
                    option(
                            "host",
                            REQUIRED,
                            keywords("vserv001", "vserv002")
                    )
            ),

            // TODO MISSING IMPL AND TEST: Option with comma separated arg list: ls --sort size,name
//            option(
//                    "--sort",
//                    list(
//                            keywords("name", "size", "ctime", "atime", "version", "extension")
//                    )
//            ),

            // Option to complicate the completion of "add u[ser]"
            option("userbad")


            // TODO: Dynamic ComplectionSpecs
//            filesAndDirectories(),
//            users(),
//            hosts()
    );

    // TODO: Use a more intelligent example, for instance git

    @Test
    public void testNoArguments() {
        final String command = "foo";
        final int tabPosition = 3;

        assertEquals(Collections.<String>emptyList(), completor.getCompletions(command, tabPosition, completionSpecRoot));
    }

    @Test
    public void testAll() {
        final String command = "foo ";
        final int tabPosition = 4;

        assertEquals(asList("--verbose", "--sort", "add", "userbad"), completor.getCompletions(command, tabPosition, completionSpecRoot));
    }

    @Test
    public void testAllNested() {
        final String command = "foo add user ";
        final int tabPosition = 13;

        assertEquals(asList("jim", "bob"), completor.getCompletions(command, tabPosition, completionSpecRoot));
    }

    @Test
    public void testOptionWithoutArguments() {
        final String command = "foo --v";
        final int tabPosition = 7;

        assertEquals(asList("--verbose"), completor.getCompletions(command, tabPosition, completionSpecRoot));
    }

    @Test
    public void testOptionWithArguments() {
        final String command = "foo bar --sort n";
        final int tabPosition = 16;

        assertEquals(asList("name", "ntestn"), completor.getCompletions(command, tabPosition, completionSpecRoot));
    }

    @Test
    public void testNestedStructures() {
        assertEquals(asList("add"), completor.getCompletions("foo a", 5, completionSpecRoot));
        assertEquals(asList("user"), completor.getCompletions("foo ignore add u", 16, completionSpecRoot));
    }
}