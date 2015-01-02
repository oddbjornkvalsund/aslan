package no.nixx.wing.core;

import no.nixx.wing.pipeline.PipelineParser;
import no.nixx.wing.pipeline.model.Pipeline;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class PipelineExecutorImplTest {

    final static PipelineParser parser = new PipelineParser();
    final static ExecutableLocatorImpl executableLocator = new ExecutableLocatorImpl();

    static ExecutorService threadPool;

    @BeforeClass
    public static void startupThreadPool() throws Exception {
        threadPool = Executors.newFixedThreadPool(4);
    }

    @AfterClass
    public static void shutdownThreadPool() throws Exception {
        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void testEmptyPipeline() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        executor.execute(new ExecutionContextImpl(), new Pipeline());

        assertEquals(format(""), out.toString());
    }

    @Test
    public void testEcho() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        executor.execute(new ExecutionContextImpl(), parser.parseCommand("echo foo"));

        assertEquals(format("foo%n"), out.toString());
    }

    @Test
    public void testPipeline() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        executor.execute(new ExecutionContextImpl(), parser.parseCommand("echo foo | grep foo"));

        assertEquals(format("foo%n"), out.toString());
    }

    @Test
    public void testVariableSubstitution() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        final ExecutionContextImpl executionContext = new ExecutionContextImpl();
        executionContext.setVariable("HOME", "MyHome");
        executor.execute(executionContext, parser.parseCommand("echo ${HOME}"));

        assertEquals(format("MyHome%n"), out.toString());
    }

    @Test
    public void testVariableSubstitutionInQuotedString() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        final ExecutionContextImpl executionContext = new ExecutionContextImpl();
        executionContext.setVariable("A", "aaa");
        executionContext.setVariable("B", "bbb");
        executionContext.setVariable("C", "ccc");
        executor.execute(executionContext, parser.parseCommand("echo \"A: ${A}, B: ${B}, C: ${C}\""));
        assertEquals(format("A: aaa, B: bbb, C: ccc%n"), out.toString());
    }

    @Test
    public void testCommandSubstitution() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        executor.execute(new ExecutionContextImpl(), parser.parseCommand("echo $(echo foo | grep o | grep o)"));

        assertEquals(format("foo%n"), out.toString());
    }

    @Test
    public void testCommandSubstitutionInQuotedString() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        final ExecutionContextImpl executionContext = new ExecutionContextImpl();
        executor.execute(executionContext, parser.parseCommand("echo \"echo foo outputs: $(echo foo)\""));

        assertEquals(format("echo foo outputs: foo%n"), out.toString());
    }

    @Test
    public void testComplexQuotedString() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, System.err);
        final ExecutionContextImpl executionContext = new ExecutionContextImpl();
        executionContext.setVariable("O", "o");
        executor.execute(executionContext, parser.parseCommand("echo \"echo foo outputs: $(echo $(echo foo | grep $(echo ${O})))\""));

        assertEquals(format("echo foo outputs: foo%n"), out.toString());
    }

    @Test
    public void testCommandSubstitutionWithErrors() {
        final InputStream in = getEmptyInputStream();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final PipelineExecutor executor = new PipelineExecutorImpl(threadPool, executableLocator, in, out, err);
        executor.execute(new ExecutionContextImpl(), parser.parseCommand("echo $(echo fooooooo | failwhenrun | grep o)"));

        // The command substitution prints 4 characters before failing. The newline is added by the first "echo"
        assertEquals(format("fooo%n"), out.toString());
    }

    private ByteArrayInputStream getEmptyInputStream() {
        return new ByteArrayInputStream(new byte[0]);
    }
}