package no.nixx.aslan.ui;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.WorkingDirectory;
import no.nixx.aslan.core.ExecutionContextFactory;
import no.nixx.aslan.core.ShellUtilExecutionContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static javafx.application.Platform.runLater;
import static no.nixx.aslan.core.utils.Preconditions.checkNotNull;

public class ObservableExecutionContextFactory implements ExecutionContextFactory {

    private final SimpleObjectProperty<WorkingDirectory> workingDirectory;
    private final StringProperty workingDirectoryBasename = new SimpleStringProperty();
    private final ObservableMap<String, String> variables = FXCollections.observableHashMap();

    public ObservableExecutionContextFactory(WorkingDirectory workingDirectory) {
        this.workingDirectory = new SimpleObjectProperty<>(workingDirectory);
        this.workingDirectoryBasename.set(getBasename(workingDirectory));
        this.workingDirectory.addListener((observable, oldValue, newValue) -> {
            runLater(() -> workingDirectoryBasename.set(getBasename(newValue)));
        });

    }

    public SimpleObjectProperty<WorkingDirectory> workingDirectoryProperty() {
        return workingDirectory;
    }

    public Property<String> workingDirectoryBasenameProperty() {
        return workingDirectoryBasename;
    }

    public ObservableMap<String, String> variablesProperty() {
        return variables;
    }

    private boolean isVariableSet(String name) {
        return variables.containsKey(checkNotNull(name));
    }

    private String getVariable(String name) {
        final String value = variables.get(name);
        if (value == null) {
            throw new IllegalArgumentException("No such variable: " + name);
        } else {
            return value;
        }
    }

    public List<String> getVariableNames() {
        final ArrayList<String> variableList = new ArrayList<>(variables.keySet());
        Collections.sort(variableList);
        return variableList;
    }

    private String getBasename(WorkingDirectory workingDirectory) {
        if (workingDirectory == null) {
            return null;
        } else {
            final Path path = workingDirectory.asPath();
            if (path.getNameCount() == 0) {
                return path.toString();
            } else {
                return path.getName(path.getNameCount() - 1).toString();
            }
        }
    }

    private void setVariable(String name, String value) {
        variables.put(checkNotNull(name), checkNotNull(value));
    }

    private void unsetVariable(String name) {
        if (variables.remove(name) == null) {
            throw new IllegalArgumentException("No such variable: " + name);
        }
    }

    public ExecutionContext createExecutionContext() {
        return new ExecutionContext() {
            @Override
            public InputStream input() {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream output() {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream error() {
                throw new UnsupportedOperationException();
            }

            @Override
            public WorkingDirectory getWorkingDirectory() {
                return workingDirectory.get();
            }

            @Override
            public boolean isVariableSet(String name) {
                return ObservableExecutionContextFactory.this.isVariableSet(name);
            }

            @Override
            public String getVariable(String name) {
                return ObservableExecutionContextFactory.this.getVariable(name);
            }

            @Override
            public List<String> getVariableNames() {
                return ObservableExecutionContextFactory.this.getVariableNames();
            }
        };
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
                return workingDirectory.get();
            }

            @Override
            public boolean isVariableSet(String name) {
                return ObservableExecutionContextFactory.this.isVariableSet(name);
            }

            @Override
            public String getVariable(String name) {
                return ObservableExecutionContextFactory.this.getVariable(name);
            }

            @Override
            public List<String> getVariableNames() {
                return ObservableExecutionContextFactory.this.getVariableNames();
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
                return workingDirectory.get();
            }

            @Override
            public void setWorkingDirectory(WorkingDirectory workingDirectory) {
                ObservableExecutionContextFactory.this.workingDirectory.set(workingDirectory);
            }

            @Override
            public boolean isVariableSet(String name) {
                return ObservableExecutionContextFactory.this.isVariableSet(name);
            }

            @Override
            public String getVariable(String name) {
                return ObservableExecutionContextFactory.this.getVariable(name);
            }

            @Override
            public List<String> getVariableNames() {
                return ObservableExecutionContextFactory.this.getVariableNames();
            }

            @Override
            public void setVariable(String name, String value) {
                ObservableExecutionContextFactory.this.setVariable(name, value);
            }

            @Override
            public void unsetVariable(String name) {
                ObservableExecutionContextFactory.this.unsetVariable(name);
            }
        };
    }
}
