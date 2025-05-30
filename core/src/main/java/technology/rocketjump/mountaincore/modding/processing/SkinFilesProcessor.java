package technology.rocketjump.mountaincore.modding.processing;

import technology.rocketjump.mountaincore.modding.exception.ModLoadingException;
import technology.rocketjump.mountaincore.modding.model.ModArtifact;
import technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition.ArtifactCombinationType.REPLACES_EXISTING;

public class SkinFilesProcessor extends ModArtifactProcessor {

	private SkinFileListing combinedListing = new SkinFileListing();

	public SkinFilesProcessor(ModArtifactDefinition definition) {
		super(definition);
	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		SkinFileListing fileListing = new SkinFileListing();
		modArtifact.sourceFiles.forEach(file -> {
			if (file.getFileName().toString().endsWith(".atlas")) {
				fileListing.atlasFile = file;
			} else if (file.getFileName().toString().endsWith(".json")) {
				fileListing.jsonFile = file;
			} else if (file.getFileName().toString().endsWith(".png")) {
				fileListing.pngFiles.add(file);
			}
		});
		modArtifact.setData(fileListing);
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException, IOException {
		if (definition.combinationType.equals(REPLACES_EXISTING)) {
			artifacts.forEach(artifact -> {
				SkinFileListing listing = (SkinFileListing) artifact.getData();
				if (listing.atlasFile != null) {
					combinedListing.atlasFile = listing.atlasFile;
				}
				if (listing.jsonFile != null) {
					combinedListing.jsonFile = listing.jsonFile;
				}
				combinedListing.pngFiles.addAll(listing.pngFiles);
			});
		} else {
			throw new RuntimeException(getClass().getSimpleName() + " only supports " + REPLACES_EXISTING.name() + " combination type (" + definition.combinationType + " specified)");
		}
	}

	@Override
	public void write(Path assetsDir) throws IOException {
		Path outputDir = assetsDir.resolve(definition.assetDir);
		if (!Files.exists(outputDir)) {
			Files.createDirectory(outputDir);
		}
		Files.copy(combinedListing.atlasFile, outputDir.resolve(definition.outputFileName + ".atlas"), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(combinedListing.jsonFile, outputDir.resolve(definition.outputFileName + ".json"), StandardCopyOption.REPLACE_EXISTING);

		for (Path pngFile : combinedListing.pngFiles) {
			Files.copy(pngFile, outputDir.resolve(pngFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static class SkinFileListing {

		private Path atlasFile;
		private Path jsonFile;
		private List<Path> pngFiles = new ArrayList<>();

	}
}
