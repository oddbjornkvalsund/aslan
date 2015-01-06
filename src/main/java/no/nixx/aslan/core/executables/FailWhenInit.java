package no.nixx.aslan.core.executables;

import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ExecutionContext;

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
