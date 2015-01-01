package no.nixx.wing.core;

import no.nixx.wing.pipeline.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class PipelineExecutorImpl implements PipelineExecutor {

    final Logger logger = LoggerFactory.getLogger(PipelineExecutorImpl.class);

    final ExecutorService threadPool;
    final ExecutableLocator executableLocator;

    final InputStream defaultInputStream;
    final OutputStream defaultOutputStream;
    final PrintStream defaultErrorStream;

    public PipelineExecutorImpl(ExecutorService threadPool, ExecutableLocator executableLocator, InputStream defaultInputStream, OutputStream defaultOutputStream, OutputStream defaultErrorStream) {
        this.threadPool = threadPool;
        this.executableLocator = executableLocator;
        this.defaultInputStream = defaultInputStream;
        this.defaultOutputStream = defaultOutputStream;
        this.defaultErrorStream =new PrintStream(defaultErrorStream);
    }

    public void execute(ExecutionContext context, Pipeline pipeline) {
        substituteVariables(context, pipeline); // TODO: Handle "string" with vs and cs and handle vs within nested pipelines
        substituteCommands(context, pipeline); // TODO: Handle "string" with vs and cs and handle vs within nested pipelines
        executeNAMETODO(context, pipeline, defaultInputStream, defaultOutputStream);
    }

    private void substituteVariables(ExecutionContext context, Pipeline pipeline) {
        for (Command command : pipeline.getCommandsUnmodifiable()) {
            for (Argument argument : command.getArgumentsUnmodifiable()) {
                if (argument.isVariableSubstitution()) {
                    command.replaceArgument(argument, getExpandedVariable(context, (VariableSubstitution) argument));
                } else if (argument.isCommandSubstitution()) {
                    substituteVariables(context, ((CommandSubstitution) argument).getPipeline());
                }
            }
        }
    }

    private Argument getExpandedVariable(ExecutionContext context, VariableSubstitution vs) {
        if (context.isVariableSet(vs.variableName)) {
            return new Literal(context.getVariable(vs.variableName));
        } else {
            throw new IllegalArgumentException("No such variable: " + vs.variableName);
        }
    }

    private void substituteCommands(ExecutionContext context, Pipeline pipeline) {
        for (Command command : pipeline.getCommandsUnmodifiable()) {
            for (Argument argument : command.getArgumentsUnmodifiable()) {
                if (argument.isCommandSubstitution()) {
                    final CommandSubstitution cs = (CommandSubstitution) argument;
                    substituteCommands(context, cs.getPipeline());
                    command.replaceArgument(argument, getExpandedCommand(context, cs.getPipeline()));
                }
            }
        }
    }

    private Argument getExpandedCommand(ExecutionContext context, Pipeline pipeline) {
        final InputStream in = new ByteArrayInputStream(new byte[0]);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        executeNAMETODO(context, pipeline, in, out);

        return new Literal(removeTrailingNewlines(out.toString()));
    }

    private void executeNAMETODO(ExecutionContext context, Pipeline pipeline, InputStream outerInputStream, OutputStream outerOutputStream) {
        final List<ExecutableWithStreams> executables = new ArrayList<>();

        // TODO: Pipeline sanity checks

        final List<Command> commands = pipeline.getCommandsUnmodifiable();
        final Command first = commands.get(0);
        final Command last = commands.get(commands.size() - 1);

        Pipe pipe = new Pipe();
        for (Command command : commands) {
            final InputStream in;
            final OutputStream out;
            if (command == first && command == last) {
                in = outerInputStream;
                out = outerOutputStream;
            } else if (command == first) {
                in = outerInputStream;
                out = pipe.getSink();
            } else if (command == last) {
                in = pipe.getSource();
                out = outerOutputStream;
            } else {
                in = pipe.getSource();
                pipe = new Pipe();
                out = pipe.getSink();
            }

            final Executable executable = executableLocator.lookupExecutable(command.getExecutableName());
            try {
                // TODO: init() runs on the main thread, which is not a good idea. It had better run on a separate and
                // interruptible thread.
                executable.init(in, out, defaultErrorStream, context, command.getArgumentsAsStrings());
            } catch (Throwable t) {
                // TODO: Direct logging output to logfile
                logger.error(t.getMessage(), t);
                defaultErrorStream.println(getExecutableName(executable) + ": " + t.getMessage());
                return;
            }
            executables.add(new ExecutableWithStreams(executable, in, out));
        }

        final CountDownLatch latch = new CountDownLatch(executables.size());
        for (final ExecutableWithStreams executableWithStreams : executables) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    final Executable executable = executableWithStreams.executable;
                    final String executableName = getExecutableName(executable);
                    try {
                        executable.run();
                    } catch (Throwable t) {
                        // TODO: Direct logging output to logfile
                        logger.error(t.getMessage(), t);
                        defaultErrorStream.println(executableName + ": " + t.getMessage());
                    } finally {
                        try {
                            if (executableWithStreams.in != defaultInputStream) {
                                executableWithStreams.in.close();
                            }

                            executableWithStreams.out.flush();

                            if (executableWithStreams.out != defaultOutputStream) {
                                executableWithStreams.out.close();
                            }
                        } catch (IOException e) {
                            System.err.println("Unable to close streams: " + e.getMessage());
                        }

                        latch.countDown();
                    }
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // TODO: Consider setting the EXITSTATUS and PIPESTATUS variables here
        // (ref. http://unix.stackexchange.com/questions/14270/get-exit-status-of-process-thats-piped-to-another)
    }

    private String getExecutableName(Executable executable) {
        final ExecutableMetadata metadata = executable.getClass().getAnnotation(ExecutableMetadata.class);
        return metadata.name();
    }

    // Consider moving this method to separate a StringUtil class if more of these methods show up...
    private static String removeTrailingNewlines(String string) {
        return string.replaceAll("[\r\n]+$", "");
    }

    private class ExecutableWithStreams {
        public final Executable executable;
        public final InputStream in;
        public final OutputStream out;

        ExecutableWithStreams(Executable executable, InputStream in, OutputStream out) {
            this.executable = executable;
            this.in = in;
            this.out = out;
        }
    }
}