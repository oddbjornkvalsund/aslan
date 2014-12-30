package no.nixx.wing.pipeline;

import no.nixx.wing.antlr.PipelineLexer;
import no.nixx.wing.antlr.PipelineParser;
import no.nixx.wing.antlr.PipelineParserBaseListener;
import no.nixx.wing.core.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.misc.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ParserTest {
    final ExecutorService threadPool = Executors.newFixedThreadPool(10); // TODO: Hard coded pool size
    final ExecutableLocator executableLocator = new ExecutableLocatorImpl();

    public static void main(String[] args) {
        new ParserTest().run();
    }

    public void run() {
        final String cmd = "ls | grep t | grep o";

        final ExecutionContext context = new ExecutionContextImpl();
        context.setVariable("CWD", System.getProperty("user.dir"));

        final PipelineLexer lexer = new PipelineLexer(new ANTLRInputStream(cmd));
        final PipelineParser parser = new PipelineParser(new BufferedTokenStream(lexer));
        final PipelineListener pipelineListener = new PipelineListener();
        parser.addParseListener(pipelineListener);
        parser.pipeline();

        final Pipeline pipeline = pipelineListener.getPipeline();
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
        final List<Executable> executables = new ArrayList<Executable>();

        // TODO: Pipeline sanity checks

        final List<Command> commands = pipeline.getCommandsUnmodifiable();
        final Command first = commands.get(0);
        final Command last = commands.get(commands.size() - 1);

        Pipe pipe = new Pipe();
        for (Command command : commands) {
            final InputStream in;
            final OutputStream out;
            if (command == first && command == last) {
                in = System.in;
                out = System.out;
            } else if (command == first) {
                in = System.in;
                out = pipe.getSink();
            } else if (command == last) {
                in = pipe.getSource();
                out = System.out;
            } else {
                in = pipe.getSource();
                pipe = new Pipe();
                out = pipe.getSink();
            }

            final Executable executable = executableLocator.lookupExecutable(command.getExecutableName());
            try {
                executable.init(in, out, System.err, context, command.getArgumentsAsStrings()); // TODO: What do we do here if this fails?
            } catch (Throwable t) {
                // TODO: Log the full exception
                System.err.println(t.getMessage());
                return;
            }
            executables.add(executable);
        }

        final CountDownLatch latch = new CountDownLatch(executables.size());
        for (final Executable executable : executables) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        executable.run();
                    } catch (Throwable t) {
                        // TODO: Log the full exception
                        System.err.println(t.getMessage());
                        // Cancel all running threads
                    } finally {
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