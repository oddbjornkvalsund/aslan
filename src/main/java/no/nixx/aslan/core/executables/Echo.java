package no.nixx.aslan.core.executables;

import no.nixx.aslan.api.ExecutionContext;
import no.nixx.aslan.api.Program;
import no.nixx.aslan.core.*;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

@ExecutableMetadata(name = "echo")
public class Echo implements Program {

    @Override
    public void run(ExecutionContext context, List<String> args) {
        final PrintWriter writer = new PrintWriter(context.output(), true);
        final Iterator<String> iterator = args.iterator();
        while (iterator.hasNext()) {
            writer.print(iterator.next());
            if (iterator.hasNext()) {
                writer.print(" ");
            }
        }
        writer.println();
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
