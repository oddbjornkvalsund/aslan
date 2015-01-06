package no.nixx.aslan.core.executables;

import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ExecutionContext;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ExecutableMetadata(name = "cat")
public class Cat implements Executable {
    private InputStream is;
    private OutputStream os;
    private ExecutionContext context;
    private List<String> args;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.is = is;
        this.os = os;
        this.context = context;
        this.args = args;
    }

    @Override
    public void run() {
        if (args.isEmpty()) {
            copy(is, os);
        } else {
            final Path cwd = Paths.get(context.getCurrentWorkingDirectory());
            for (String filename : args) {
                if (filename.equals("-")) {
                    copy(is, os);
                } else {
                    final Path file = cwd.resolve(Paths.get(filename));
                    try {
                        final FileInputStream fileInputStream = new FileInputStream(file.toFile());
                        copy(fileInputStream, os);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void copy(InputStream in, OutputStream out) {
        try {
            final byte[] buffer = new byte[4096];
            while (true) {
                final int bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    break;
                } else {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
