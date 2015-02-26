package no.nixx.aslan.core;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.WorkingDirectory;

public interface ShellUtilExecutionContext extends ExecutionContext {
    void setWorkingDirectory(WorkingDirectory workingDirectory);

    void setVariable(String name, String value);

    void unsetVariable(String name);

}
