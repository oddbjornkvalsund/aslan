package no.nixx.aslan.core.completion;

import java.util.List;

public final class CompletionSpecRoot extends CompletionSpec {

    public CompletionSpecRoot(CompletionSpec... children) {
        super(children);
    }

    @Override
    public CompletionSpec getParent() {
        throw new IllegalStateException();
    }

    @Override
    public boolean isPartialMatch(String argument) {
        return false;
    }

    @Override
    public boolean isCompleteMatch(String argument) {
        return false;
    }

    @Override
    public List<String> getCompletions(String argument) {
        throw new IllegalStateException();
    }

}