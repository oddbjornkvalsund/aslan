package no.nixx.wing.core;

import java.util.HashMap;

public class ExecutionContextImpl implements ExecutionContext {

    private String currentWorkingDirectory = System.getProperty("user.dir");
    private HashMap<String, String> variables = new HashMap<String, String>();

    @Override
    public String getCurrentWorkingDirectory() {
        return currentWorkingDirectory;
    }

    @Override
    public void setCurrentWorkingDirectory(String currentWorkingDirectory) {
        this.currentWorkingDirectory = currentWorkingDirectory;
    }

    @Override
    public boolean isVariableSet(String name) {
        return variables.containsKey(name);
    }

    @Override
    public String getVariable(String name) {
        return variables.get(name);
    }

    @Override
    public void setVariable(String name, String value) {
        variables.put(name, value);
    }

    @Override
    public void unsetVariable(String name) {
        variables.remove(name);
    }
}