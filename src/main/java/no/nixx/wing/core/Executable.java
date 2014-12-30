package no.nixx.wing.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface Executable {
    void init(InputStream is, OutputStream os, OutputStream es, Map<String, String> env, List<String> args);

    void run() throws IOException;

    int getExitStatus();
}
