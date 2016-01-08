package no.nixx.aslan.core.executables.shellutils;

import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.ShellUtil;
import no.nixx.aslan.core.ShellUtilExecutionContext;

import java.io.PrintWriter;
import java.util.List;

@ExecutableMetadata(name = "pwd")
public class Pwd implements ShellUtil {
    @Override
    public void run(ShellUtilExecutionContext context, List<String> args) {
        final PrintWriter writer = new PrintWriter(context.output(), true);
        writer.println(context.getWorkingDirectory().asPath().toAbsolutePath());
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}
