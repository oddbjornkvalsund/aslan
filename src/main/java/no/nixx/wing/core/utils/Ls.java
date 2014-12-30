package no.nixx.wing.core.utils;

import no.nixx.wing.core.Executable;
import no.nixx.wing.core.ExecutableMetadata;
import no.nixx.wing.core.ExecutionContext;

import java.io.*;
import java.util.List;

@ExecutableMetadata(name = "ls")
public class Ls implements Executable {
    private InputStream is;
    private OutputStream os;
    private OutputStream es;
    private ExecutionContext context;
    private List<String> args;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.is = is;
        this.os = os;
        this.es = es;
        this.context = context;
        this.args = args;
    }

    @Override
    public void run() throws IOException {
        final PrintWriter pw = new PrintWriter(os);

        final File currentWorkingDirectory = new File(context.getVariable("CWD"));
        if (currentWorkingDirectory.exists() && currentWorkingDirectory.isDirectory()) {
            for (String filename : currentWorkingDirectory.list()) {
                final File file = new File(filename);

                if (file.isDirectory()) {
                    pw.println(filename + File.separator);
                } else {
                    pw.println(filename);
                }
            }
        }

        is.close();
        pw.close();
        os.close();
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}