package no.nixx.wing.core;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class Pipe {
    private final PipedInputStream source = new PipedInputStream();
    private final PipedOutputStream sink = new PipedOutputStream();

    public Pipe() {
        try {
            source.connect(sink);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public PipedInputStream getSource() {
        return source;
    }

    public PipedOutputStream getSink() {
        return sink;
    }
}
