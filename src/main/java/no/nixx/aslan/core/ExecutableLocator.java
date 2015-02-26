package no.nixx.aslan.core;

import no.nixx.aslan.api.Executable;

import java.util.List;

public interface ExecutableLocator {
    Executable lookupExecutable(String name);

    List<String> findExecutableCandidates(String name);
}
