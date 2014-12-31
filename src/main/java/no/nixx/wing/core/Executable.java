package no.nixx.wing.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface Executable {
    void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args);

    void run();

    int getExitStatus();
}
