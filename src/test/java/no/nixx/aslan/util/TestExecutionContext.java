package no.nixx.aslan.util;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.WorkingDirectory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class TestExecutionContext implements ExecutionContext {

    private final WorkingDirectory workingDirectory;

    public TestExecutionContext(WorkingDirectory workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public InputStream input() {
        return null;
    }

    @Override
    public OutputStream output() {
        return null;
    }

    @Override
    public OutputStream error() {
        return null;
    }

    @Override
    public WorkingDirectory getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public boolean isVariableSet(String name) {
        return false;
    }

    @Override
    public String getVariable(String name) {
        return "";
    }

    @Override
    public List<String> getVariableNames() {
        return Collections.emptyList();
    }
}