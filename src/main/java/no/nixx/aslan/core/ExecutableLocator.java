package no.nixx.aslan.core;

import java.util.List;

public interface ExecutableLocator {
    Executable lookupExecutable(String name);

    List<String> findExecutableCandidates(String name);
}
