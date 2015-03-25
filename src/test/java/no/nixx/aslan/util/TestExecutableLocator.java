package no.nixx.aslan.util;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.core.ExecutableLocator;
import no.nixx.aslan.core.completion.CompletionSpecRoot;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class TestExecutableLocator implements ExecutableLocator {

    private final String defaultExecutableName;
    private final CompletionSpecRoot completionSpecRoot;

    public TestExecutableLocator(String defaultExecutableName, CompletionSpecRoot completionSpecRoot) {
        this.defaultExecutableName = defaultExecutableName;
        this.completionSpecRoot = completionSpecRoot;
    }

    @Override
    public Executable lookupExecutable(String name) {
        return (name.equals(defaultExecutableName)) ? new TestExecutable(completionSpecRoot) : null;
    }

    @Override
    public List<String> findExecutableCandidates(String name) {
        return defaultExecutableName.startsWith(name) ? asList(defaultExecutableName) : Collections.<String>emptyList();
    }
}