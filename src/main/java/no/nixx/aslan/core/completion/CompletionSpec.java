package no.nixx.aslan.core.completion;

import no.nixx.aslan.core.completion.specs.KeywordCompletionSpec;
import no.nixx.aslan.core.completion.specs.OptionCompletionSpec;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public abstract class CompletionSpec {

    private final List<CompletionSpec> children;
    private CompletionSpec parent;

    public CompletionSpec(CompletionSpec... children) {
        this.children = unmodifiableList(asList(children));
        for (CompletionSpec child : this.children) {
            child.setParent(this);
        }
    }

    // Factory methods
    public static OptionCompletionSpec option(String name, CompletionSpec... children) {
        return new OptionCompletionSpec(name, children);
    }

    public static CompletionSpec keywords(String... keywords) {
        return new KeywordCompletionSpec(keywords);
    }

    public abstract boolean isPartialMatch(String argument);

    public abstract boolean isCompleteMatch(String argument);

    public abstract List<String> getCompletions(String argument);

    public boolean canOccurOnlyOnce() {
        return true;
    }

    public boolean appendQuoteAndSpaceIfOnlyOneCompletion() {
        return true;
    }

    public CompletionSpec getParent() {
        if (parent == null) {
            throw new IllegalStateException("Parent not set!");
        }
        return parent;
    }

    public void setParent(CompletionSpec parent) {
        this.parent = parent;
        for (CompletionSpec child : this.children) {
            child.setParent(this);
        }
    }

    public List<CompletionSpec> getChildren() {
        return children;
    }
}