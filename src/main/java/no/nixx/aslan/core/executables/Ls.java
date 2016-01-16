package no.nixx.aslan.core.executables;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.*;
import no.nixx.aslan.core.completion.Completable;
import no.nixx.aslan.core.completion.CompletionSpecRoot;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;

@ExecutableMetadata(name = "ls")
public class Ls implements Program, Completable {
    @Override
    public void run(ExecutionContext context, List<String> args) {
        final PrintWriter pw = new PrintWriter(context.output(), false);
        final Path cwd = context.getWorkingDirectory().asPath();
        if (exists(cwd) && isDirectory(cwd)) {
            try {
                Files.list(cwd).forEach(f -> pw.println(f.getFileName() + (isDirectory(f) ? File.separator : "")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                pw.flush();
            }
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }

    @Override
    public CompletionSpecRoot getCompletionSpec(ExecutionContext executionContext) {
        return new CompletionSpecRoot();
    }
}