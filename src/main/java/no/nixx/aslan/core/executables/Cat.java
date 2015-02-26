package no.nixx.aslan.core.executables;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ExecutableMetadata(name = "cat")
public class Cat implements Program {
    @Override
    public void run(ExecutionContext context, List<String> args) {
        if (args.isEmpty()) {
            copy(context.input(), context.output());
        } else {
            final Path cwd = context.getWorkingDirectory().asPath();
            for (String filename : args) {
                if (filename.equals("-")) {
                    copy(context.input(), context.output());
                } else {
                    final Path file = cwd.resolve(Paths.get(filename));
                    try {
                        final FileInputStream fileInputStream = new FileInputStream(file.toFile());
                        copy(fileInputStream, context.output());
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
