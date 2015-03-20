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
import static no.nixx.aslan.core.completion.specs.PathCompletionSpec.Type.FILES;

public class PathCompletionSpec extends CompletionSpec {

    private final static String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    private final ExecutionContext executionContext;
    private final Type type;
    private boolean doAppendSpace = false;

    public PathCompletionSpec(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.type = Type.FILES_AND_DIRECTORIES;
    }

    public PathCompletionSpec(ExecutionContext executionContext, Type type) {
        this.executionContext = executionContext;
        this.type = type;
    }

    @Override
    public boolean isPartialMatch(String argument) {
        return getMatchingPaths(argument).findAny().isPresent();
    }

    @Override
    public boolean isCompleteMatch(String argument) {
        final Path resolvedArgument = getResolved(argument);
        final boolean exists = Files.exists(resolvedArgument);
        final boolean hasCorrectType;
        switch (type) {
            case FILES:
                hasCorrectType = Files.isRegularFile(resolvedArgument);
                break;
            case DIRECTORIES:
                hasCorrectType = Files.isDirectory(resolvedArgument);
                break;
            case FILES_AND_DIRECTORIES:
                hasCorrectType = Files.isRegularFile(resolvedArgument) || Files.isDirectory(resolvedArgument);
                break;
            default:
                hasCorrectType = false;
        }

        return exists && hasCorrectType;
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
            final TypeFilter typeFilter = new TypeFilter(type);

            if (argument.isEmpty()) {
                return Files
                        .list(workingDirectory)
                        .filter(typeFilter::filterByType)
                        .map(p -> p.subpath(workingDirectory.getNameCount(), p.getNameCount()));
            } else {
                final Path path = Paths.get(argument);
                final Path absolutePath = getResolved(argument);
                final NameFilter nameFilter = new NameFilter(absolutePath.getFileName());

                final Stream<Path> stream;
                if (Files.exists(absolutePath)) {
                    if (Files.isDirectory(absolutePath)) {
                        if (argument.endsWith(FILE_SEPARATOR)) {
                            stream = Files
                                    .list(absolutePath)
                                    .filter(typeFilter::filterByType);
                        } else {
                            stream = Files
                                    .list(absolutePath.getParent())
                                    .filter(nameFilter::filterByName)
                                    .filter(typeFilter::filterByType);
                        }
                    } else {
                        stream = Stream.of(absolutePath);
                    }
                } else {
                    stream = Files
                            .list(absolutePath.getParent())
                            .filter(nameFilter::filterByName)
                            .filter(typeFilter::filterByType);
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
        return "PathCompletionSpec{" +
                "executionContext=" + executionContext +
                '}';
    }

    public enum Type {
        FILES,
        DIRECTORIES,
        FILES_AND_DIRECTORIES
    }
}

class TypeFilter {
    private final PathCompletionSpec.Type type;

    public TypeFilter(PathCompletionSpec.Type type) {
        this.type = type;
    }

    public boolean filterByType(Path p) {
        if (type == PathCompletionSpec.Type.DIRECTORIES) {
            return Files.isDirectory(p);
        } else if (type == FILES) {
            return Files.isRegularFile(p);
        } else {
            return true;
        }
    }
}

class NameFilter {
    private final Path pathToMatch;

    public NameFilter(Path pathToMatch) {
        this.pathToMatch = pathToMatch;
    }

    public boolean filterByName(Path p) {
        return p.getFileName().toString().startsWith(pathToMatch.toString());
    }

}