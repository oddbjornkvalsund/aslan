package no.nixx.aslan.core.executables;

import no.nixx.aslan.core.Executable;
import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ExecutionContext;

import java.io.*;
import java.util.List;

import static no.nixx.aslan.core.utils.ListUtils.firstOf;

@ExecutableMetadata(name = "grep")
public class Grep implements Executable {
    private InputStream is;
    private OutputStream os;

    private String pattern;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.is = is;
        this.os = os;

        if (args.size() == 1) {
            pattern = firstOf(args);
        } else {
            throw new IllegalArgumentException("Only one pattern allowed!");
        }
    }

    @Override
    public void run() {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        final PrintWriter pw = new PrintWriter(os, true);

        try {
            while (true) {
                final String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }

                if (line.contains(pattern)) { // TODO: REGEX
                    pw.println(line);
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