package no.nixx.aslan.core;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.WorkingDirectory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class ExecutionContextFactoryImpl implements ExecutionContextFactory {

    private final Map<String, String> variables = new HashMap<>();

    private WorkingDirectory workingDirectory;

    public ExecutionContextFactoryImpl(WorkingDirectory initialWorkingDirectory, Map<String, String>... initialVariableSets) {
        this.workingDirectory = checkNotNull(initialWorkingDirectory);
        for (Map<String, String> initialVariableSet : checkNotNull(initialVariableSets)) {
            this.variables.putAll(checkNotNull(initialVariableSet));
        }
    }

    public boolean isVariableSet(String name) {
        return variables.containsKey(checkNotNull(name));
    }

    public String getVariable(String name) {
        final String value = variables.get(name);
        if (value == null) {
            throw new IllegalArgumentException("No such variable: " + name);
        } else {
            return value;
        }
    }

    public List<String> getVariableNames() {
        final ArrayList<String> variableNames = new ArrayList<>(variables.keySet());
        Collections.sort(variableNames);
        return variableNames;
    }

    public void setVariable(String name, String value) {
        variables.put(checkNotNull(name), checkNotNull(value));
    }

    public void unsetVariable(String name) {
        if (variables.remove(name) == null) {
            throw new IllegalArgumentException("No such variable: " + name);
        }
    }

    @Override
    public ExecutionContext createExecutionContext(InputStream input, OutputStream output, OutputStream error) {
        checkNotNull(input);
        checkNotNull(output);
        checkNotNull(error);

        return new ExecutionContext() {
            @Override
            public InputStream input() {
                return input;
            }

            @Override
            public OutputStream output() {
                return output;
            }

            @Override
            public OutputStream error() {
                return error;
            }

            @Override
            public WorkingDirectory getWorkingDirectory() {
                return workingDirectory;
            }

            @Override
            public boolean isVariableSet(String name) {
                return ExecutionContextFactoryImpl.this.isVariableSet(name);
            }

            @Override
            public String getVariable(String name) {
                return ExecutionContextFactoryImpl.this.getVariable(name);
            }

            @Override
            public List<String> getVariableNames() {
                return ExecutionContextFactoryImpl.this.getVariableNames();
            }
        };
    }

    @Override
    public ShellUtilExecutionContext createShellUtilExecutionContext(InputStream input, OutputStream output, OutputStream error) {
        checkNotNull(input);
        checkNotNull(output);
        checkNotNull(error);

        return new ShellUtilExecutionContext() {
            @Override
            public InputStream input() {
                return input;
            }

            @Override
            public OutputStream output() {
                return output;
            }

            @Override
            public OutputStream error() {
                return error;
            }

            @Override
            public WorkingDirectory getWorkingDirectory() {
                return workingDirectory;
            }

            @Override
            public void setWorkingDirectory(WorkingDirectory workingDirectory) {

            }

            @Override
            public boolean isVariableSet(String name) {
                return ExecutionContextFactoryImpl.this.isVariableSet(name);
            }

            @Override
            public String getVariable(String name) {
                return ExecutionContextFactoryImpl.this.getVariable(name);
            }

            @Override
            public List<String> getVariableNames() {
                return ExecutionContextFactoryImpl.this.getVariableNames();
            }

            @Override
            public void setVariable(String name, String value) {
                ExecutionContextFactoryImpl.this.setVariable(name, value);
            }

            @Override
            public void unsetVariable(String name) {
                ExecutionContextFactoryImpl.this.unsetVariable(name);
            }
        };
    }
}