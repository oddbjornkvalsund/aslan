package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.completion.CompletionSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class PathCompletionSpec extends CompletionSpec {

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
        final String pathSeparator = "\\"; // TODO: Portable
        final List<Path> matchingPaths = getMatchingPaths(argument).collect(toList());

        doAppendSpace = (matchingPaths.size() == 1) && Files.isRegularFile(getResolved(matchingPaths.get(0)));

        return matchingPaths.stream().map(p -> Files.isDirectory(getResolved(p)) ? p.toString() + pathSeparator : p.toString()).collect(toList());
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
        final Path workingDirectory = executionContext.getWorkingDirectory().asPath();

        final Path argumentAsPath = Paths.get(argument);
        System.out.println("argumentAsPath: " + argumentAsPath);
        System.out.println("Absolute: " + argumentAsPath.isAbsolute());

        try {
            if (argument.isEmpty()) {
                return Files
                        .list(workingDirectory)
                        .map(p -> p.subpath(workingDirectory.getNameCount(), p.getNameCount()));
            } else {
                final Path fullPath = getResolved(argument);
                if (Files.exists(fullPath) && Files.isDirectory(fullPath)) {
                    if (fullPath.isAbsolute()) {
                        return Files.
                                list(fullPath);
                    } else {
                        return Files.
                                list(fullPath)
                                .map(p -> p.subpath(workingDirectory.getNameCount(), p.getNameCount()));
                    }
                } else {
                    final Path dir = fullPath.getParent();
                    final String partialPath = fullPath.getFileName().toString();
                    if (dir.isAbsolute()) {
                        return Files
                                .list(dir)
                                .filter(p -> p.getFileName().toString().startsWith(partialPath));
                    } else {
                        return Files
                                .list(dir)
                                .filter(p -> p.getFileName().toString().startsWith(partialPath))
                                .map(p -> p.subpath(workingDirectory.getNameCount(), p.getNameCount()));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getResolved(String str) {
        return executionContext.getWorkingDirectory().asPath().resolve(str);
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