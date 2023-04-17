package technology.rocketjump.mountaincore.modding.processing;

import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.modding.exception.ModLoadingException;
import technology.rocketjump.mountaincore.modding.model.ModArtifact;
import technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CopyFilesProcessor extends ModArtifactProcessor {

	private Map<String, Path> combined = new LinkedHashMap<>();

	public CopyFilesProcessor(ModArtifactDefinition definition) {
		super(definition);
	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		// Do nothing
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException {
		switch (definition.combinationType) {
			case ADDITIVE:
				// Still need to replace on name
				for (ModArtifact artifact : artifacts) {
					for (Path sourceFile : artifact.sourceFiles) {
						String filename = sourceFile.getFileName().toString();
						if (combined.containsKey(filename)) {
							Logger.info("Replacing " + (definition.classType == null ? "" : definition.classType.getSimpleName() + " ") + filename);
						}
						combined.put(filename, sourceFile);
					}
				}
				break;
			case REPLACES_EXISTING:
				for (ModArtifact artifact : artifacts) {
					if (artifact.sourceFiles.size() != 1) {
						throw new ModLoadingException("Expecting only a single file");
					}
					combined.put("single", artifact.sourceFiles.get(0));
				}
				break;
			default:
				throw new RuntimeException("Not yet implemented, combinationType " + definition.combinationType + " for " + getClass().getSimpleName());
		}
	}

	@Override
	public void write(Path assetsDir) throws IOException {
		switch (definition.outputType) {
			case COPY_ORIGINAL_FILES: {
				Path outputDir = assetsDir.resolve(definition.assetDir);
				if (!Files.exists(outputDir)) {
					Files.createDirectory(outputDir);
				}
				if (containsDir(outputDir)) {
					throw new RuntimeException("Need to rearrange assets so files of this artifact are in their own directory");
				}

				try (Stream<Path> fileList = Files.list(outputDir)) {
					fileList.forEach(file -> {
						try {
							Files.delete(file);
						} catch (IOException e) {
							Logger.error(e, "Could not delete existing file");
						}
					});
				}

				for (Path inputFile : combined.values()) {
					Path outputFile = outputDir.resolve(inputFile.getFileName());
					if (!Files.exists(outputFile)) {
						Files.copy(inputFile, outputFile);
					}
				}
				break;
			}
			case SINGLE_FILE:
				Path outputDir = assetsDir.resolve(definition.assetDir);
				if (!Files.exists(outputDir)) {
					throw new RuntimeException(outputDir + " should exist");
				}

				if (combined.values().size() != 1) {
					throw new RuntimeException(definition.getName() + " should have a single file defined");
				}
				Path inputFile = combined.values().iterator().next();
				Path targetFile = outputDir.resolve(inputFile.getFileName());
				Files.deleteIfExists(targetFile);
				Files.copy(inputFile, outputDir.resolve(inputFile.getFileName()));
				break;
			default:
				throw new RuntimeException("Not yet implemented: " + definition.outputType);
		}

	}

	private boolean containsDir(Path outputDir) throws IOException {
		try (Stream<Path> fileList = Files.list(outputDir)) {
			return fileList.anyMatch(Files::isDirectory);
		}
	}

}
