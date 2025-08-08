package technology.rocketjump.mountaincore.assets.editor;

import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.persistence.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        }

        defaultSettings = Paths.get(FileUtils.getDirectory(foundFiles.get(0)).toString(), "Default.preset").toString();

        if (! System.getProperty("os.name", "unknown").toLowerCase().contains("win")) {
            // attempt to discover path of laigter on Linux/...
            String[] whichCmd = {"command", "-v", "laigter"};
            try {
                Process which = Runtime.getRuntime().exec(whichCmd);
                if(0 == which.waitFor()) {
                    BufferedReader whichOutputStream = new BufferedReader(new InputStreamReader(which.getInputStream()));
                    foundFiles = whichOutputStream.lines().map(Path::of).toList();
                } else {
                    BufferedReader whichErrorStream = new BufferedReader(new InputStreamReader(which.getErrorStream()));
                    throw new RuntimeException("Failed 'command -v laigter' on non-Windows OS, exit code: " + which.exitValue() + ", stderr: " + String.join("\n", whichErrorStream.lines().toList()));
                }
            } catch (Exception ex) {
                throw new RuntimeException("Cannot find laigter in PATH on non-Windows OS (get it from https://github.com/azagaya/laigter)", ex);
            }
        }

        laigterExe = foundFiles.get(0).toString();
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
