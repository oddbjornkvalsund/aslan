package no.nixx.wing.core;

import no.nixx.wing.core.executables.*;

import java.util.HashMap;
import java.util.Map;

public class ExecutableLocatorImpl implements ExecutableLocator {

    final Map<String, Class<? extends Executable>> map = new HashMap<String, Class<? extends Executable>>() {
        {
            put("cd", Cd.class);
            put("ls", Ls.class);
            put("grep", Grep.class);
            put("echo", Echo.class);
            put("cat", Cat.class);
            put("failwhenrun", FailWhenRun.class);
            put("failwheninit", FailWhenInit.class);
        }
    };

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
            throw new RuntimeException("No such executable: " + name);
        }
    }
}