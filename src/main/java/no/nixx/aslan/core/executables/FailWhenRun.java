package no.nixx.aslan.core.executables;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.*;

import java.io.IOException;
import java.util.List;

@ExecutableMetadata(name = "failwhenrun")
public class FailWhenRun implements Program {

    @Override
    public void run(ExecutionContext context, List<String> args) {
        final byte[] buffer = new byte[4];
        try {
            context.input().read(buffer);
            context.output().write(buffer);
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