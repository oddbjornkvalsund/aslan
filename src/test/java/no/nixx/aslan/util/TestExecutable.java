package no.nixx.aslan.util;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.completion.Completable;
import no.nixx.aslan.core.completion.CompletionSpecRoot;

import java.util.List;

public class TestExecutable implements Program, Completable {

    private final CompletionSpecRoot completionSpecRoot;

    public TestExecutable(CompletionSpecRoot completionSpecRoot) {
        this.completionSpecRoot = completionSpecRoot;
    }

    @Override
    public CompletionSpecRoot getCompletionSpec(ExecutionContext executionContext) {
        return this.completionSpecRoot;
    }

    @Override
    public void run(ExecutionContext executionContext, List<String> args) {
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
