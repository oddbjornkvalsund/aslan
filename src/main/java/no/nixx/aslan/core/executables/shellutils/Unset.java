package no.nixx.aslan.core.executables.shellutils;

import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ShellUtil;
import no.nixx.aslan.core.ShellUtilExecutionContext;

import java.io.PrintWriter;
import java.util.List;

@ExecutableMetadata(name = "unset")
public class Unset implements ShellUtil {
    @Override
    public void run(ShellUtilExecutionContext context, List<String> args) {
        if (args.isEmpty()) {
            final PrintWriter printWriter = new PrintWriter(context.output());
            for (String name : context.getVariableNames()) {
                printWriter.format("%s=%s\n", name, context.getVariable(name));
            }
            printWriter.flush();
        } else if (args.size() == 1) {
            context.unsetVariable(args.get(0));
        } else {
            throw new IllegalArgumentException("Only one variable can be unset!");
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
