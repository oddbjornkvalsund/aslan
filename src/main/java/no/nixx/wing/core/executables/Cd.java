package no.nixx.wing.core.executables;

import no.nixx.wing.core.Executable;
import no.nixx.wing.core.ExecutableMetadata;
import no.nixx.wing.core.ExecutionContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.Files.isDirectory;

@ExecutableMetadata(name = "cd")
public class Cd implements Executable {
    private ExecutionContext context;

    private String nextWorkingDirectory;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.context = context;

        if (args.size() == 1) {
            final Path path = Paths.get(context.getCurrentWorkingDirectory()).resolve(Paths.get(args.get(0)));
            if (isDirectory(path)) {
                nextWorkingDirectory = path.toString();
            } else {
                throw new IllegalArgumentException("Not a directory: " + path.toString());
            }
        } else {
            throw new IllegalArgumentException("Only one dir can be set!");
        }
    }

    @Override
    public void run() {
        context.setCurrentWorkingDirectory(nextWorkingDirectory);
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}