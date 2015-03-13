package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.WorkingDirectoryImpl;
import no.nixx.aslan.core.completion.CompletionResult;
import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.Completor;
import no.nixx.aslan.util.TestExecutableLocator;
import no.nixx.aslan.util.TestExecutionContext;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class PathCompletionSpecTest {

    final Path fsRoot = createDirectory(Paths.get("."), "target/fsRoot");
    final Completor completor = new Completor();
    final ExecutionContext executionContext = new TestExecutionContext(new WorkingDirectoryImpl(fsRoot));
    final CompletionSpecRoot completionSpec = new CompletionSpecRoot(new PathCompletionSpec(executionContext));
    final ExecutableLocator executableLocator = new TestExecutableLocator("ls", completionSpec);

    @Before
    public void setUp() throws Exception {
        deleteDirectoryRecursively(fsRoot);

        final Path foo = createDirectory(fsRoot, "foo");
        createFile(foo, "Notes.txt");
        createFile(foo, "Notes_old.txt");

        final Path bar = createDirectory(fsRoot, "bar");
        createFile(bar, "Image.png");
        createFile(bar, "Image_old.png");
        createFile(bar, "Imaginary.png");
    }

    @Test
    public void testPathCompletionSpec() {
        CompletionResult completions;

        System.out.println("fsRoot: " + fsRoot);
        // TODO: Make path separators portable!

        completions = completor.getCompletions("ls ", 3, executableLocator, executionContext);
        assertEquals(new CompletionResult(3, "ls ", asList("bar\\", "foo\\")), completions);

        completions = completor.getCompletions("ls f", 4, executableLocator, executionContext);
        assertEquals(new CompletionResult(7, "ls foo\\", asList()), completions);

        completions = completor.getCompletions("ls b", 4, executableLocator, executionContext);
        assertEquals(new CompletionResult(7, "ls bar\\", asList()), completions);

        completions = completor.getCompletions("ls bar\\", 7, executableLocator, executionContext);
        assertEquals(new CompletionResult(11, "ls bar\\Imag", asList("bar\\Image.png", "bar\\Image_old.png", "bar\\Imaginary.png")), completions);

        completions = completor.getCompletions("ls bar\\Imag", 11, executableLocator, executionContext);
        assertEquals(new CompletionResult(11, "ls bar\\Imag", asList("bar\\Image.png", "bar\\Image_old.png", "bar\\Imaginary.png")), completions);

        completions = completor.getCompletions("ls C:\\", 6, executableLocator, executionContext);
        System.out.println(completions);
    }

    private Path createDirectory(Path parent, String name) {
        final Path newDirectory = parent.resolve(name);
        try {
            return Files.exists(newDirectory) ? newDirectory : Files.createDirectories(newDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path createFile(Path dir, String name) throws IOException {
        return Files.createFile(dir.resolve(name));
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("Deleting file: " + file);
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                System.out.println("Deleting dir: " + dir);
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }
}