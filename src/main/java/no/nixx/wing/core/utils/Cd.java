package no.nixx.wing.core.utils;

import no.nixx.wing.core.Executable;
import no.nixx.wing.core.ExecutableMetadata;
import no.nixx.wing.core.ExecutionContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ExecutableMetadata(name = "cd")
public class Cd implements Executable {
    private InputStream is;
    private OutputStream os;
    private OutputStream es;
    private ExecutionContext context;
    private List<String> args;

    private Path currentWorkingDirectory;
    private Path nextWorkingDirectory;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        // TODO: Parse options
        this.is = is;
        this.os = os;
        this.es = es;
        this.context = context;
        this.args = args;

        if (context.isVariableSet("CWD")) {
            currentWorkingDirectory = Paths.get(context.getVariable("CWD")).toAbsolutePath();
        } else {
            throw new IllegalArgumentException("Unable to determine current working directory!");
        }

        if (args.size() == 1) {
            final Path path = currentWorkingDirectory.resolve(Paths.get(args.get(0)));
            if (path.toFile().isDirectory()) {
                nextWorkingDirectory = path;
            } else {
                throw new IllegalArgumentException("Not a directory: " + path.toString());
            }
        } else {
            throw new IllegalArgumentException("Only one dir can be set!");
        }
    }

    @Override
    public void run() throws IOException {
        is.close();
        os.close();
        context.setVariable("CWD", nextWorkingDirectory.toString());
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}