package no.nixx.wing.core.utils;

import no.nixx.wing.core.Executable;
import no.nixx.wing.core.ExecutableMetadata;
import no.nixx.wing.core.ExecutionContext;

import java.io.*;
import java.util.List;

@ExecutableMetadata(name = "grep")
public class Grep implements Executable {
    private InputStream is;
    private OutputStream os;
    private OutputStream es;
    private ExecutionContext context;
    private List<String> args;

    private String pattern;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        this.is = is;
        this.os = os;
        this.es = es;
        this.context = context;
        this.args = args;

        if (args.size() == 1) {
            pattern = args.get(0);
        } else {
            throw new IllegalArgumentException("Only one pattern allowed!");
        }
    }

    @Override
    public void run() throws IOException {
        final InputStreamReader inputStreamReader = new InputStreamReader(is);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        final PrintWriter pw = new PrintWriter(os);

        while (true) {
            final String line = bufferedReader.readLine();
            if (line == null) {
                bufferedReader.close();
                inputStreamReader.close();
                is.close();
                pw.flush();
                pw.close();
                os.close();
                break;
            }

            if (line.contains(pattern)) { // TODO: REGEX
                pw.println(line);
            }
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
