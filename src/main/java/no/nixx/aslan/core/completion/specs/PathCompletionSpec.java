package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.completion.CompletionSpec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class PathCompletionSpec extends CompletionSpec {

    private final static String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

    private final ExecutionContext executionContext;

    private boolean doAppendSpace = false;

    public PathCompletionSpec(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public boolean isPartialMatch(String argument) {
        return getMatchingPaths(argument).findAny().isPresent();
    }

    @Override
    public boolean isCompleteMatch(String argument) {
        return true;
    }

    @Override
    public List<String> getCompletions(String argument) {
        final List<Path> matchingPaths = getMatchingPaths(argument).collect(toList());

        doAppendSpace = (matchingPaths.size() == 1) && Files.isRegularFile(getResolved(matchingPaths.get(0)));

        return matchingPaths.stream().map(p -> Files.isDirectory(getResolved(p)) ? p.toString() + FILE_SEPARATOR : p.toString()).collect(toList());
    }

    @Override
    public boolean canOccurOnlyOnce() {
        return false;
    }

    @Override
    public boolean appendSpaceIfOnlyOneCompletion() {
        return doAppendSpace;
    }

    private Stream<Path> getMatchingPaths(String argument) {
        final Path workingDirectory = executionContext.getWorkingDirectory().asPath().toAbsolutePath();

        try {
            if (argument.isEmpty()) {
                return Files.list(workingDirectory).map(p -> p.subpath(workingDirectory.getNameCount(), p.getNameCount()));
            } else {
                final Path path = Paths.get(argument);
                final Path absolutePath = getResolved(argument);
                final Stream<Path> stream;
                if (Files.exists(absolutePath)) {
                    if (Files.isDirectory(absolutePath)) {
                        if (argument.endsWith(FILE_SEPARATOR)) {
                            stream = Files.list(absolutePath);
                        } else {
                            stream = Stream.of(absolutePath);
                        }
                    } else {
                        stream = Stream.empty();
                    }
                } else {
                    stream = Files.list(absolutePath.getParent()).filter(p -> p.getFileName().toString().startsWith(absolutePath.getFileName().toString()));
                }

                if (path.isAbsolute() || absolutePath.getNameCount() == 0) {
                    return stream;
                } else if (isDriveAbsolute(path)) {
                    final Path driveRoot = Paths.get(FILE_SEPARATOR);
                    return stream.map(p -> driveRoot.resolve(p.subpath(0, p.getNameCount())));
                } else {
                    return stream.map(p -> p.subpath(workingDirectory.getNameCount(), p.getNameCount()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isDriveAbsolute(Path path) {
        return !path.isAbsolute() && path.toString().startsWith(FILE_SEPARATOR);
    }

    private Path getResolved(String str) {
        return executionContext.getWorkingDirectory().asPath().toAbsolutePath().resolve(str);
    }

    private Path getResolved(Path path) {
        return executionContext.getWorkingDirectory().asPath().resolve(path);
    }

    @Override
    public String toString() {
        return "FilesCompletionSpec{" +
                "executionContext=" + executionContext +
                '}';
    }
}