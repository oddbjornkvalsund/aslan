package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.WorkingDirectoryImpl;
import no.nixx.aslan.core.completion.CompletionResult;
import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.Completor;
import no.nixx.aslan.util.TestExecutableLocator;
import no.nixx.aslan.util.TestExecutionContext;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class PathCompletionSpecTest {

    // TODO: Make path separators portable!

    final static Path fsRoot = createDirectory(Paths.get(".").toAbsolutePath().normalize(), "target/fsRoot");
    final static Path fsRootAbsolute = fsRoot.toAbsolutePath();

    final Completor completor = new Completor();
    final ExecutionContext executionContext = new TestExecutionContext(new WorkingDirectoryImpl(fsRoot));
    final CompletionSpecRoot completionSpec = new CompletionSpecRoot(new PathCompletionSpec(executionContext));
    final ExecutableLocator executableLocator = new TestExecutableLocator("ls", completionSpec);

    @BeforeClass
    public static void setUp() throws Exception {
        deleteDirectoryRecursively(fsRoot);

        final Path foo = createDirectory(fsRoot, "foo");
        createFile(foo, "Notes.txt");
        createFile(foo, "Notes_old.txt");

        final Path foobar = createDirectory(fsRoot, "foobar");
        createFile(foobar, "Foobar.txt");
        createFile(foobar, "Foobar_old.txt");

        final Path bar = createDirectory(fsRoot, "bar");
        createFile(bar, "Image.png");
        createFile(bar, "Image_old.png");
        createFile(bar, "Imaginary.png");
    }

    private static Path createDirectory(Path parent, String name) {
        final Path newDirectory = parent.resolve(name);
        try {
            return Files.exists(newDirectory) ? newDirectory : Files.createDirectories(newDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path createFile(Path dir, String name) throws IOException {
        return Files.createFile(dir.resolve(name));
    }

    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    public void testRelativeDirPathCompletion() {
        CompletionResult completions;

        // Empty -> Match all dirs -> list all with trailing slash
        completions = completor.getCompletions("ls ", 3, executableLocator, executionContext);
        assertEquals(new CompletionResult(3, "ls ", asList("bar\\", "foo\\", "foobar\\")), completions);

        // Partial match of one dir -> complete with trailing slash
        completions = completor.getCompletions("ls b", 4, executableLocator, executionContext);
        assertEquals(new CompletionResult(7, "ls bar\\", asList()), completions);

        // Partial match of one dir starting with slash -> complete with trailing slash
        completions = completor.getCompletions("ls \\Us", 6, executableLocator, executionContext);
        assertEquals(new CompletionResult(10, "ls \\Users\\", asList()), completions);

        // Fully match of one dir -> complete with trailing slash
        completions = completor.getCompletions("ls bar", 6, executableLocator, executionContext);
        assertEquals(new CompletionResult(7, "ls bar\\", asList()), completions);

        // Fully match of one dir whose name is also contained in other dirs name -> list all with trailing slash
        completions = completor.getCompletions("ls foo", 6, executableLocator, executionContext);
        assertEquals(new CompletionResult(6, "ls foo", asList("foo\\", "foobar\\")), completions);

        // Fully match of one dir with trailing slash -> list all with trailing slash
        completions = completor.getCompletions("ls foo\\", 7, executableLocator, executionContext);
        assertEquals(new CompletionResult(12, "ls foo\\Notes", asList("foo\\Notes.txt", "foo\\Notes_old.txt")), completions);

        // Partial match of one file -> complete with trailing space
        completions = completor.getCompletions("ls foo\\Notes.", 13, executableLocator, executionContext);
        assertEquals(new CompletionResult(17, "ls foo\\Notes.txt ", asList()), completions);

        // Fully match of one file -> complete with trailing space
        completions = completor.getCompletions("ls foo\\Notes.txt", 16, executableLocator, executionContext);
        assertEquals(new CompletionResult(17, "ls foo\\Notes.txt ", asList()), completions);
    }

    @Test
    public void testAbsoluteDirPathCompletion() {
        CompletionResult completions;
        String command;

        // Empty -> Match all dirs -> does not make sense for absolute paths

        // Partial match of one dir -> complete with trailing slash
        command = "ls " + resolveAbsolute("b");
        completions = completor.getCompletions(command, command.length(), executableLocator, executionContext);
        assertEquals("ls " + resolveAbsoluteWithTrailingSlash("bar"), completions.text);
        assertEquals(Collections.<String>emptyList(), completions.completionCandidates);

        // Fully match of one dir -> complete with trailing slash
        command = "ls " + resolveAbsolute("bar");
        completions = completor.getCompletions(command, command.length(), executableLocator, executionContext);
        assertEquals("ls " + resolveAbsoluteWithTrailingSlash("bar"), completions.text);
        assertEquals(Collections.<String>emptyList(), completions.completionCandidates);

        // Fully match of one dir whose name is also contained in other dirs name -> list all with trailing slash
        command = "ls " + resolveAbsolute("foo");
        completions = completor.getCompletions(command, command.length(), executableLocator, executionContext);
        assertEquals("ls " + resolveAbsolute("foo"), completions.text);
        assertEquals(asList(resolveAbsoluteWithTrailingSlash("foo"), resolveAbsoluteWithTrailingSlash("foobar")), completions.completionCandidates);

        // Fully match of one dir with trailing slash -> list all with trailing slash
        command = "ls " + resolveAbsoluteWithTrailingSlash("foo");
        completions = completor.getCompletions(command, command.length(), executableLocator, executionContext);
        assertEquals("ls " + resolveAbsolute("foo\\Notes"), completions.text);
        assertEquals(asList(resolveAbsolute("foo\\Notes.txt"), resolveAbsolute("foo\\Notes_old.txt")), completions.completionCandidates);
    }

    private String resolveAbsolute(String path) {
        return fsRootAbsolute.resolve(path).toString();
    }

    private String resolveAbsoluteWithTrailingSlash(String path) {
        return fsRootAbsolute.resolve(path).toString() + "\\";
    }
}