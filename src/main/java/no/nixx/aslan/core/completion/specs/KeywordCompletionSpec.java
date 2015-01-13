package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.core.completion.CompletionSpec;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class KeywordCompletionSpec extends CompletionSpec {

    private final List<String> keywords;

    public KeywordCompletionSpec(String... keywords) {
        this.keywords = asList(keywords);
    }

    @Override
    public boolean isPartialMatch(String argument) {
        return (argument.isEmpty() || getCompletions(argument).size() > 0) && !isCompleteMatch(argument);
    }

    @Override
    public boolean isCompleteMatch(String argument) {
        return keywords.contains(argument);
    }

    @Override
    public List<String> getCompletions(String argument) {
        return keywords.stream().filter((s) -> s.startsWith(argument)).collect(toList());

    }
}
