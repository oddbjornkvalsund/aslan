package no.nixx.aslan.core.completion;

import no.nixx.aslan.core.completion.specs.KeywordCompletionSpec;
import no.nixx.aslan.core.completion.specs.OptionCompletionSpec;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public abstract class CompletionSpec {

    private final boolean argumentRequired;
    private final List<CompletionSpec> children;
    private CompletionSpec parent;

    public CompletionSpec(CompletionSpec... children) {
        this.argumentRequired = false;
        this.children = unmodifiableList(asList(children));
        for (CompletionSpec child : this.children) {
            child.setParent(this);
        }
    }

    public CompletionSpec(ArgumentRequirement argumentRequired, CompletionSpec... children) {
        this.argumentRequired = argumentRequired == ArgumentRequirement.REQUIRED;
        this.children = unmodifiableList(asList(children));
        for (CompletionSpec child : this.children) {
            child.setParent(this);
        }
    }

    public static OptionCompletionSpec option(String name, CompletionSpec... children) {
        return new OptionCompletionSpec(name, children);
    }

    public static OptionCompletionSpec option(String name, ArgumentRequirement argumentRequirement, CompletionSpec... children) {
        return new OptionCompletionSpec(name, argumentRequirement, children);
    }

    public static CompletionSpec list(CompletionSpec... children) {
        return null;
    }

    public static CompletionSpec keywords(String... keywords) {
        return new KeywordCompletionSpec(keywords);
    }

    public static CompletionSpec files() {
        return null;
    }

    public static CompletionSpec directories() {
        return null;
    }

    public static CompletionSpec filesAndDirectories() {
        return null;
    }

    public static CompletionSpec users() {
        return null;
    }

    public static CompletionSpec hosts() {
        return null;
    }

    // Factory methods

    public abstract boolean isPartialMatch(String argument);

    public abstract boolean isCompleteMatch(String argument);

    public abstract List<String> getCompletions(String argument);

    public boolean isArgumentRequired() {
        return argumentRequired;
    }

    // TODO

    public boolean hasParent() {
        return parent != null;
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

    public static enum ArgumentRequirement {
        OPTIONAL,
        REQUIRED
    }

}
