package technology.rocketjump.saul.persistence;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileUtils {
    /**
     * Get directory ignoring the file
     * @param input Path to a file or directory
     * @return directory if input is file
     */
    public static Path getDirectory(Path input) {
        boolean isDirectory = Files.isDirectory(input);
        if (!isDirectory) {
            return input.getParent();
        } else {
            return input;
        }
    }

    /**
     * Creates a directory if one doesn't exist
     * @throws UncheckedIOException when something has gone wrong creating the directory
     * @param parent the parent folder for the new directory
     * @param folder name of the directory you want to create
     * @return full path of the new directory
     */
    public static Path createDirectory(Path parent, String folder) {
        Path directoryToCreate = Paths.get(parent.toString(), folder);
        if (!Files.exists(directoryToCreate)) {
            try {
                Files.createDirectories(directoryToCreate);
            } catch (IOException e) {
                String message = "Failed to create directory " + folder + " due to " + e.getMessage();
                Logger.error(e, message);
                throw new UncheckedIOException(message, e);
            }
        }
        return directoryToCreate;
    }

    //TODO: sort duplication
    public static List<Path> findFilesByFilename(Path directory, Pattern pattern) {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(modFile -> modFile.getFileName().toString().matches(pattern.pattern())).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<Path> findFilesByFilename(Path directory, String filename) {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(modFile -> modFile.getFileName().toString().equals(filename)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public static void copy(Path source, Path target) {
        try {
            Files.copy(source, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
