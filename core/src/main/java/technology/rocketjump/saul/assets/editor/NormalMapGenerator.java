package technology.rocketjump.saul.assets.editor;

import com.google.inject.Singleton;
import technology.rocketjump.saul.persistence.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class NormalMapGenerator {

    private final String laigterExe;
    private final String defaultSettings;

    public NormalMapGenerator() {
        Path directory = Paths.get("mod_tools");
        List<Path> foundFiles = FileUtils.findFilesByFilename(directory, "laigter.exe");//TODO: not sure on this design

        if (foundFiles.isEmpty()) {
            throw new RuntimeException("Cannot find laigter.exe in " + directory.toAbsolutePath());
        } else {
            laigterExe = foundFiles.get(0).toString();
            defaultSettings = Paths.get(FileUtils.getDirectory(foundFiles.get(0)).toString(), "Default.preset").toString();
        }
    }

    public Path generate(Path inputImageFile) {
        Path workingDirectory = FileUtils.getDirectory(inputImageFile);
        String imageFileName = inputImageFile.getFileName().toString();
        String nameWithoutExtension = imageFileName.substring(0, imageFileName.lastIndexOf('.'));
        String extension = imageFileName.substring(imageFileName.lastIndexOf('.'));
        String expectedFileName = nameWithoutExtension + "_NORMALS" + extension;
        //TODO: don't like this code here
        if (workingDirectory.resolve(expectedFileName).toFile().exists()) {
            return workingDirectory.resolve(expectedFileName);
        }


        ProcessBuilder processBuilder = new ProcessBuilder(laigterExe, "--no-gui", "-n", "-r", defaultSettings, "-d", imageFileName);
        processBuilder.directory(workingDirectory.toFile());
        try {
            Process process = processBuilder.start();
            boolean terminated = process.waitFor(5, TimeUnit.SECONDS);
            if (terminated) {
                //todo: error logging
                if (process.exitValue() == 0) {
                    String generatedFileName = nameWithoutExtension + "_n" + extension;
                    Path normalFile = workingDirectory.resolve(expectedFileName);
                    Files.move(workingDirectory.resolve(generatedFileName), normalFile);

                    return normalFile;
                } else  {
                    throw new RuntimeException("Normal map generation failed: exit value " + process.exitValue());
                }
            } else {
                throw new RuntimeException("Could not generate a normal map in the allocated time");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
