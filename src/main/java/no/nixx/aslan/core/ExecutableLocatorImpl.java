package no.nixx.aslan.core;

import no.nixx.aslan.api.Executable;
import no.nixx.aslan.core.executables.*;
import no.nixx.aslan.core.executables.shellutils.Cd;
import no.nixx.aslan.core.executables.shellutils.Set;
import no.nixx.aslan.core.executables.shellutils.Unset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutableLocatorImpl implements ExecutableLocator {

    final Map<String, Class<? extends Executable>> map = new HashMap<>();

    {
        map.put("cd", Cd.class);
        map.put("ls", Ls.class);
        map.put("grep", Grep.class);
        map.put("echo", Echo.class);
        map.put("cat", Cat.class);
        map.put("set", Set.class);
        map.put("unset", Unset.class);
        map.put("failwhenrun", FailWhenRun.class);
    }

    @Override
    public Executable lookupExecutable(String name) {
        if (map.containsKey(name)) {
            final Class<? extends Executable> executableClass = map.get(name);
            try {
                return executableClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public List<String> findExecutableCandidates(String partialName) {
        return map.keySet().stream().filter(e -> e.startsWith(partialName)).collect(Collectors.toList());
    }
}