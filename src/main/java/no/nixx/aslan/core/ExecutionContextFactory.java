package no.nixx.aslan.core;

import no.nixx.aslan.api.ExecutionContext;

import java.io.InputStream;
import java.io.OutputStream;

public interface ExecutionContextFactory {
    ExecutionContext createExecutionContext(InputStream input, OutputStream output, OutputStream error);

    ShellUtilExecutionContext createShellUtilExecutionContext(InputStream input, OutputStream output, OutputStream error);
}
