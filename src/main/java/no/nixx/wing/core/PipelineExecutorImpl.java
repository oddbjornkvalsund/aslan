package no.nixx.wing.core;

import no.nixx.wing.pipeline.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PipelineExecutorImpl implements PipelineExecutor {

    final static Logger logger = LoggerFactory.getLogger(PipelineExecutorImpl.class);

    final ExecutorService threadPool;
    final ExecutableLocator executableLocator;

    final InputStream defaultInputStream;
    final OutputStream defaultOutputStream;
    final PrintStream defaultErrorStream;

    // This is intended for testing
    public PipelineExecutorImpl() {
        this.threadPool = Executors.newFixedThreadPool(10);
        this.executableLocator = new ExecutableLocatorImpl();
        this.defaultInputStream = System.in;
        this.defaultOutputStream = System.out;
        this.defaultErrorStream = System.err;
    }

    public PipelineExecutorImpl(ExecutorService threadPool, ExecutableLocator executableLocator, InputStream defaultInputStream, OutputStream defaultOutputStream, PrintStream defaultErrorStream) {
        this.threadPool = threadPool;
        this.executableLocator = executableLocator;
        this.defaultInputStream = defaultInputStream;
        this.defaultOutputStream = defaultOutputStream;
        this.defaultErrorStream = defaultErrorStream;
    }

    public void execute(ExecutionContext context, Pipeline pipeline) {
        substituteVariables(context, pipeline); // TODO: Handle "string" with vs and cs and handle vs within nested pipelines
        substituteCommands(pipeline); // TODO: Not implemented
        executeNAMETODO(context, pipeline);

        // TODO: Pipeline should be immutable and substitution of variables/commands should result in a new Pipeline-instance
    }

    private void substituteVariables(ExecutionContext context, Pipeline pipeline) {
        for (Command command : pipeline.getCommandsUnmodifiable()) {
            for (Argument argument : command.getArgumentsUnmodifiable()) {
                if (argument.isVariableSubstitution()) {
                    command.replaceArgument(argument, getExpandedVariable(context, (VariableSubstitution) argument));
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

    private void substituteCommands(@SuppressWarnings("UnusedParameters") Pipeline pipeline) {

    }

    private String getExecutableName(Executable executable) {
        final ExecutableMetadata metadata = executable.getClass().getAnnotation(ExecutableMetadata.class);
        return metadata.name();
    }

    private void executeNAMETODO(ExecutionContext context, Pipeline pipeline) {
        final List<ExecutableWithStreams> executables = new ArrayList<ExecutableWithStreams>();

        // TODO: Pipeline sanity checks

        final List<Command> commands = pipeline.getCommandsUnmodifiable();
        final Command first = commands.get(0);
        final Command last = commands.get(commands.size() - 1);

        Pipe pipe = new Pipe();
        for (Command command : commands) {
            final InputStream in;
            final OutputStream out;
            if (command == first && command == last) {
                in = defaultInputStream;
                out = defaultOutputStream;
            } else if (command == first) {
                in = defaultInputStream;
                out = pipe.getSink();
            } else if (command == last) {
                in = pipe.getSource();
                out = defaultOutputStream;
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