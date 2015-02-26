package no.nixx.aslan.core.executables;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.*;
import no.nixx.aslan.core.completion.Completable;
import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.specs.KeywordCompletionSpec;

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
        final PrintWriter pw = new PrintWriter(context.output(), true);
        final Path cwd = context.getWorkingDirectory().asPath();
        if (exists(cwd) && isDirectory(cwd)) {
            try {
                Files.list(cwd).forEach(f -> pw.println(f.getFileName() + (isDirectory(f) ? File.separator : "")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }

    @Override
    public CompletionSpecRoot getCompletionSpec() {
        return new CompletionSpecRoot(
                new KeywordCompletionSpec("fileA", "fileB", "fileC", "foo", "bar") {
                    @Override
                    public boolean canOccurOnlyOnce() {
                        return false;
                    }
                }
        );
    }
}