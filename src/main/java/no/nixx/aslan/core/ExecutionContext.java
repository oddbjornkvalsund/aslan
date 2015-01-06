package no.nixx.aslan.core;

public interface ExecutionContext {
    String getCurrentWorkingDirectory();

    void setCurrentWorkingDirectory(String currentWorkingDirectory);

    boolean isVariableSet(String name);

    String getVariable(String name);

    void setVariable(String name, String value);

    void unsetVariable(String name);
}
