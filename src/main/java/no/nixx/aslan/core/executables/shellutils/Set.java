package no.nixx.aslan.core.executables.shellutils;

import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ShellUtil;
import no.nixx.aslan.core.ShellUtilExecutionContext;

import java.io.PrintWriter;
import java.util.List;

import static no.nixx.aslan.core.utils.ListUtils.firstOf;

@ExecutableMetadata(name = "set")
public class Set implements ShellUtil {
    @Override
    public void run(ShellUtilExecutionContext context, List<String> args) {
        if (args.isEmpty()) {
            final PrintWriter printWriter = new PrintWriter(context.output());
            for (String name : context.getVariableNames()) {
                printWriter.format("%s=%s\n", name, context.getVariable(name));
            }
            printWriter.flush();
        } else if (args.size() == 1) {
            final String statement = firstOf(args);
            if (statement.matches("^\\w+=\\w+$")) {
                final String[] tokens = statement.split("=");
                context.setVariable(tokens[0], tokens[1]);
            } else {
                throw new IllegalArgumentException("Unable to parse variable statement: " + statement);
            }
        } else {
            throw new IllegalArgumentException("Only one variable can be set!");
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
