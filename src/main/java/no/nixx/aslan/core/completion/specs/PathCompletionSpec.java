package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.core.completion.CompletionSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        doAppendSpace = (matchingPaths.size() == 1) && Files.isRegularFile(matchingPaths.get(0));

        return matchingPaths.stream().map(p -> Files.isDirectory(p) ? p.getFileName().toString() + pathSeparator : p.getFileName().toString()).collect(toList());
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
        final Path dir = argument.isEmpty() ? workingDirectory : getArgumentResolved(argument).getParent();
        final String partialPath = argument.isEmpty() ? "" : getArgumentResolved(argument).getFileName().toString();

        try {
            return Files.list(dir).filter(p -> p.getFileName().toString().startsWith(partialPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getArgumentResolved(String argument) {
        return executionContext.getWorkingDirectory().asPath().resolve(argument);
    }

    @Override
    public String toString() {
        return "FilesCompletionSpec{" +
                "executionContext=" + executionContext +
                '}';
    }
}