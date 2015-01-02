package no.nixx.wing.core.executables;

import no.nixx.wing.core.Executable;
import no.nixx.wing.core.ExecutableMetadata;
import no.nixx.wing.core.ExecutionContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

@ExecutableMetadata(name = "echo")
public class Echo implements Executable {

    private PrintWriter writer;
    private List<String> args;

    @Override
    public void init(InputStream is, OutputStream os, OutputStream es, ExecutionContext context, List<String> args) {
        writer = new PrintWriter(os, true);
        this.args = args;
    }

    @Override
    public void run() {
        final Iterator<String> iterator = args.iterator();
        while(iterator.hasNext()) {
            writer.print(iterator.next());
            if(iterator.hasNext()) {
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
