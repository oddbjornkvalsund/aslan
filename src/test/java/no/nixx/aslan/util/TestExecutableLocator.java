package no.nixx.aslan.util;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.completion.Completable;
import no.nixx.aslan.core.completion.CompletionSpecRoot;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class TestExecutableLocator implements ExecutableLocator {

    private final String defaultExecutableName;
    private final CompletionSpecRoot completionSpec;

    public TestExecutableLocator(String defaultExecutableName, CompletionSpecRoot completionSpec) {
        this.defaultExecutableName = defaultExecutableName;
        this.completionSpec = completionSpec;
    }

    @Override
    public Executable lookupExecutable(String name) {
        return (name.equals(defaultExecutableName)) ? new TestExecutable() : null;
    }

    @Override
    public List<String> findExecutableCandidates(String name) {
        return defaultExecutableName.startsWith(name) ? asList(defaultExecutableName) : Collections.<String>emptyList();
    }

    class TestExecutable implements Program, Completable {

        @Override
        public CompletionSpecRoot getCompletionSpec(ExecutionContext executionContext) {
            return completionSpec;
        }

        @Override
        public void run(ExecutionContext executionContext, List<String> args) {
        }

        @Override
        public int getExitStatus() {
            return 0;
        }
    }
}