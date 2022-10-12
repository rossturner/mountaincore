package technology.rocketjump.saul.modding.processing;

import technology.rocketjump.saul.modding.exception.ModLoadingException;
import technology.rocketjump.saul.modding.model.ModArtifact;
import technology.rocketjump.saul.modding.model.ModArtifactDefinition;
import technology.rocketjump.saul.modding.model.ParsedMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static technology.rocketjump.saul.modding.model.ModArtifactDefinition.ArtifactCombinationType.REPLACES_EXISTING;

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
				fileListing.pngFile = file;
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
				if (listing.pngFile != null) {
					combinedListing.pngFile = listing.pngFile;
				}
			});
		} else {
			throw new RuntimeException(getClass().getSimpleName() + " only supports " + REPLACES_EXISTING.name() + " combination type (" + definition.combinationType + " specified)");
		}
	}

	@Override
	public void write(Path assetsDir) throws IOException {
		Path outputDir = assetsDir.resolve(definition.assetDir);
		Files.copy(combinedListing.atlasFile, outputDir.resolve(definition.outputFileName + ".atlas"), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(combinedListing.jsonFile, outputDir.resolve(definition.outputFileName + ".json"), StandardCopyOption.REPLACE_EXISTING);
		Files.copy(combinedListing.pngFile, outputDir.resolve(definition.outputFileName + ".png"), StandardCopyOption.REPLACE_EXISTING);
	}

	private static class SkinFileListing {

		private Path atlasFile;
		private Path jsonFile;
		private Path pngFile;

	}
}
