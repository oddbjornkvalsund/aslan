package no.nixx.wing.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import no.nixx.wing.core.ExecutionContext;

public class ObservableExecutionContext implements ExecutionContext {

    private SimpleStringProperty currentWorkingDirectoryProperty = new SimpleStringProperty("");

    @Override
    public String getCurrentWorkingDirectory() {
        return currentWorkingDirectoryProperty.get();
    }

    @Override
    public void setCurrentWorkingDirectory(String currentWorkingDirectory) {
        currentWorkingDirectoryProperty.set(currentWorkingDirectory);
    }

    public StringProperty currentWorkingDirectoryProperty() {
        return currentWorkingDirectoryProperty;
    }

    @Override
    public boolean isVariableSet(String name) {
        return false;
    }

    @Override
    public String getVariable(String name) {
        return null;
    }

    @Override
    public void setVariable(String name, String value) {

    }

    @Override
    public void unsetVariable(String name) {

    }
}
