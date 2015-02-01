package no.nixx.aslan.core.completion.specs;

import no.nixx.aslan.core.completion.CompletionSpec;

import java.util.List;

import static java.util.Arrays.asList;

public class OptionCompletionSpec extends CompletionSpec {

    private final String name;

    public OptionCompletionSpec(String name, CompletionSpec... children) {
        super(children);
        this.name = name;
    }

    @Override
    public boolean isPartialMatch(String argument) {
        return name.startsWith(argument) && !isCompleteMatch(argument);
    }

    @Override
    public boolean isCompleteMatch(String argument) {
        return name.equals(argument);
    }

    @Override
    public List<String> getCompletions(String argument) {
        return asList(name);
    }

    @Override
    public String toString() {
        return "OptionCompletionSpec{" +
                "name='" + name + '\'' +
                '}';
    }
}