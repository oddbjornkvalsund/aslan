package no.nixx.aslan.core.executables.shellutils;

import no.nixx.aslan.core.ShellUtilExecutionContext;
import no.nixx.aslan.core.ShellUtil;
import no.nixx.aslan.core.ExecutableMetadata;
import no.nixx.aslan.core.WorkingDirectoryImpl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.Files.isDirectory;
import static no.nixx.aslan.core.utils.ListUtils.firstOf;

@ExecutableMetadata(name = "cd")
public class Cd implements ShellUtil {

    @Override
    public void run(ShellUtilExecutionContext context, List<String> args) {
        if (args.size() == 1) {
            final Path path = context.getWorkingDirectory().asPath().resolve(Paths.get(firstOf(args))).normalize(); // TODO: Put this in WorkingDirectory
            if (isDirectory(path)) {
                context.setWorkingDirectory(new WorkingDirectoryImpl(path));
            } else {
                throw new IllegalArgumentException("Not a directory: " + path.toString());
            }
        } else {
            throw new IllegalArgumentException("Only one dir can be set!");
        }
    }

    @Override
    public int getExitStatus() {
        return 0;
    }
}