package no.nixx.wing.core.executables;

import no.nixx.wing.core.Executable;
import no.nixx.wing.core.ExecutableMetadata;
import no.nixx.wing.core.ExecutionContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@ExecutableMetadata(name = "failwheninit")
public class FailWhenInit implements Executable {
    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        throw new RuntimeException("Failed in init()!");
    }

    @Override
    public void run() {
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
