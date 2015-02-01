package no.nixx.aslan.completion.specs;

import no.nixx.aslan.core.completion.CompletionSpec;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class TestFilesCompletionSpec extends CompletionSpec {

    private final List<String> files;

    public TestFilesCompletionSpec(String... files) {
        this.files = asList(files);
    }

    @Override
    public boolean isPartialMatch(String argument) {
        return (argument.isEmpty() || getCompletions(argument).size() > 0) && !isCompleteMatch(argument);
    }

    @Override
    public boolean isCompleteMatch(String argument) {
        return files.contains(argument);
    }

    @Override
    public List<String> getCompletions(String argument) {
        return files.stream().filter((s) -> s.startsWith(argument)).collect(toList());
    }

    public boolean canOccurOnlyOnce() {
        return false;
    }

    @Override
    public String toString() {
        return "FilesCompletionSpec{" +
                "files=" + files +
                '}';
    }
}