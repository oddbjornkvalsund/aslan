package no.nixx.aslan.core.executables;

import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ExecutionContext;
import no.nixx.aslan.core.completion.Completable;
import no.nixx.aslan.core.completion.CompletionSpecRoot;
import no.nixx.aslan.core.completion.specs.KeywordCompletionSpec;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

@ExecutableMetadata(name = "ls")
public class Ls implements Executable, Completable {
    private OutputStream os;
    private ExecutionContext context;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.os = os;
        this.context = context;
    }

    @Override
    public void run() {
        final PrintWriter pw = new PrintWriter(os, true);

        final File currentWorkingDirectory = new File(context.getCurrentWorkingDirectory());
        if (currentWorkingDirectory.exists() && currentWorkingDirectory.isDirectory()) {
            for (String filename : currentWorkingDirectory.list()) {
                final File file = new File(currentWorkingDirectory, filename);

                if (file.isDirectory()) {
                    pw.println(filename + File.separator);
                } else {
                    pw.println(filename);
                }
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