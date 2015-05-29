package no.nixx.aslan.core.completion;

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

    public abstract boolean isPartialMatch(String argument);

    public abstract boolean isCompleteMatch(String argument);

    public abstract List<String> getCompletions(String argument);

    public boolean canOccurOnlyOnce() {
        return true;
    }

    public boolean appendSpaceIfOnlyOneCompletion() {
        return true;
    }

    public boolean appendQuoteIfOnlyOneCompletion() {
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