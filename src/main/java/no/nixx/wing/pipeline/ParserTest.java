package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.PipelineLexer;
import no.nixx.wing.antlr.PipelineParser;
import no.nixx.wing.antlr.PipelineParserBaseListener;
import no.nixx.wing.core.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParserTest {
    final static Logger logger = LoggerFactory.getLogger(ParserTest.class);

    final static InputStream DEFAULT_IN = System.in;
    final static OutputStream DEFAULT_OUT = System.out;

    final ExecutorService threadPool = Executors.newFixedThreadPool(10); // TODO: Hard coded pool size
    final ExecutableLocator executableLocator = new ExecutableLocatorImpl();

    public static void main(String[] args) {
        new ParserTest().run();
    }

    public void run() {
        final ExecutionContext context = new ExecutionContextImpl();
        context.setCurrentWorkingDirectory(System.getProperty("user.dir"));

        final String cmd = "ls | failwheninit | grep i";
        final Pipeline pipeline = parseCommand(cmd);
        substituteVariables(context, pipeline); // TODO: Handle "string" with vs and cs
        substituteCommands(pipeline); // TODO: Not implemented
        execute(context, pipeline);

        try {
            threadPool.shutdown();
            threadPool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Pipeline parseCommand(String cmd) {
        final PipelineLexer lexer = new PipelineLexer(new ANTLRInputStream(cmd));
        final PipelineParser parser = new PipelineParser(new BufferedTokenStream(lexer));
        final PipelineListener pipelineListener = new PipelineListener();
        parser.addParseListener(pipelineListener);
        parser.pipeline();

        return pipelineListener.getPipeline();
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

    private void execute(ExecutionContext context, Pipeline pipeline) {
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
                in = DEFAULT_IN;
                out = DEFAULT_OUT;
            } else if (command == first) {
                in = DEFAULT_IN;
                out = pipe.getSink();
            } else if (command == last) {
                in = pipe.getSource();
                out = DEFAULT_OUT;
            } else {
                in = pipe.getSource();
                pipe = new Pipe();
                out = pipe.getSink();
            }

            final Executable executable = executableLocator.lookupExecutable(command.getExecutableName());
            try {
                executable.init(in, out, System.err, context, command.getArgumentsAsStrings());
            } catch (Throwable t) {
                // TODO: Direct logging output to logfile
                logger.error(t.getMessage(), t);
                System.err.println(getExecutableName(executable) + ": " + t.getMessage());
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
                        System.err.println(executableName + ": " + t.getMessage());
                    } finally {
                        try {
                            if (executableWithStreams.in != DEFAULT_IN) {
                                executableWithStreams.in.close();
                            }

                            executableWithStreams.out.flush();

                            if (executableWithStreams.out != DEFAULT_OUT) {
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
}

class ExecutableWithStreams {
    public final Executable executable;
    public final InputStream in;
    public final OutputStream out;

    ExecutableWithStreams(Executable executable, InputStream in, OutputStream out) {
        this.executable = executable;
        this.in = in;
        this.out = out;
    }
}

class PipelineListener extends PipelineParserBaseListener {

    final Pipeline pipeline = new Pipeline();

    private Command currentCommand; // TODO: This must be a Stack<Command>

    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void enterCmd(@NotNull PipelineParser.CmdContext ctx) {
        currentCommand = new Command();
    }

    @Override
    public void exitCmd(@NotNull PipelineParser.CmdContext ctx) {
        pipeline.addCommand(currentCommand);
    }

    @Override
    public void exitArg(@NotNull PipelineParser.ArgContext ctx) {
        if (ctx.ARG() != null) {
            currentCommand.addArgument(new Literal(ctx.ARG().getText()));
        }
    }

    @Override
    public void exitLiteral(@NotNull PipelineParser.LiteralContext ctx) {
        currentCommand.addArgument(new Literal(ctx.LT_TEXT().getText()));
    }

    @Override
    public void exitVs(@NotNull PipelineParser.VsContext ctx) {
        currentCommand.addArgument(new VariableSubstitution(ctx.VS_VARIABLE().getText()));
    }

    @Override
    public void enterCs(@NotNull PipelineParser.CsContext ctx) {
        // TODO
    }

    @Override
    public void exitCs(@NotNull PipelineParser.CsContext ctx) {
        // TODO
    }

    @Override
    public void enterText(@NotNull PipelineParser.TextContext ctx) {
        // TODO
    }

    @Override
    public void exitText(@NotNull PipelineParser.TextContext ctx) {
        // TODO
    }
}