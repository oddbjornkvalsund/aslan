package no.nixx.aslan.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface ExecutionContext {
    InputStream input();

    OutputStream output();

    OutputStream error();

    WorkingDirectory getWorkingDirectory();

    boolean isVariableSet(String name);

    String getVariable(String name);

    List<String> getVariableNames();

}