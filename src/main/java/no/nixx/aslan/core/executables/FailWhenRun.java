package no.nixx.aslan.core.executables;

import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ExecutionContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@ExecutableMetadata(name = "failwhenrun")
public class FailWhenRun implements Executable {
    private InputStream is;
    private OutputStream os;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.is = is;
        this.os = os;
    }

    @Override
    public void run() {
        final byte[] buffer = new byte[4];
        try {
            is.read(buffer);
            os.write(buffer);
            throw new RuntimeException("Read 4 bytes and failed!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}