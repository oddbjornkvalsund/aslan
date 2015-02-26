package no.nixx.aslan.core.executables;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.*;

import java.io.*;
import java.util.List;

import static no.nixx.aslan.core.utils.Preconditions.checkArgument;

@ExecutableMetadata(name = "grep")
public class Grep implements Program {

    @Override
    public void run(ExecutionContext context, List<String> args) {
        checkArgument(args.size() == 1);
        final String pattern = args.get(0);
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.input()));
        final PrintWriter pw = new PrintWriter(context.output(), true);

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