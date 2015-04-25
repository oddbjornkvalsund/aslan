package no.nixx.aslan.core;

import no.nixx.aslan.api.WorkingDirectory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WorkingDirectoryImpl implements WorkingDirectory {

    private final Path path;

    public WorkingDirectoryImpl(String path) {
        this(Paths.get(path));
    }

    public WorkingDirectoryImpl(Path path) {
        if (Files.isDirectory(path)) {
            this.path = path;
        } else {
            throw new IllegalArgumentException("Not a directory: " + path);
        }
    }

    @Override
    public Path asPath() {
        return path;
    }
}
