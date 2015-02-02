package no.nixx.aslan.core.executables;

import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ExecutionContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.Files.isDirectory;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;

@ExecutableMetadata(name = "cd")
public class Cd implements Executable {
    private ExecutionContext context;
    private List<String> args;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.context = context;
        this.args = args;
    }

    @Override
    public void run() {
        if (args.size() == 1) {
            final Path path = Paths.get(context.getCurrentWorkingDirectory()).resolve(Paths.get(firstOf(args))).normalize();
            if (isDirectory(path)) {
                context.setCurrentWorkingDirectory(path.toString());
            } else {
                throw new IllegalArgumentException("Not a directory: " + path.toString());
            }
        } else {
            throw new IllegalArgumentException("Only one dir can be set!");
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}