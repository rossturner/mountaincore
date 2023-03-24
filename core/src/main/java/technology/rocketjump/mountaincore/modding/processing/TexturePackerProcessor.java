package technology.rocketjump.mountaincore.modding.processing;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.modding.exception.ModLoadingException;
import technology.rocketjump.mountaincore.modding.model.ModArtifact;
import technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TexturePackerProcessor extends ModArtifactProcessor {

	private Path packJsonPath = null;
	private Path diffuseDir;
	private Path normalsDir;

	public TexturePackerProcessor(ModArtifactDefinition definition) {
		super(definition);
	}

	@Override
	public void apply(ModArtifact modArtifact, ParsedMod mod, Path assetDir) throws ModLoadingException {
		if (definition.getPackJsonPath() == null) {
			throw new RuntimeException(definition.getName() + " must have a pack.json specified");
		}

		Path packJsonPath = mod.getBasePath().resolve(definition.getPackJsonPath());
		if (Files.exists(packJsonPath)) {
			modArtifact.setData(packJsonPath);
		}
	}

	@Override
	public void combine(List<ModArtifact> artifacts, Path tempDir) throws ModLoadingException, IOException {
		diffuseDir = createTempSubDir(tempDir, "diffuse");
		normalsDir = createTempSubDir(tempDir, "normals");

		for (ModArtifact artifact : artifacts) {
			Path packJsonPath = (Path) artifact.getData();
			if (packJsonPath != null) {
				this.packJsonPath = packJsonPath;
			}

			for (Path sourceFile : artifact.sourceFiles) {

				Path targetDir = diffuseDir;
				String sourceFileName = sourceFile.getFileName().toString();
				if (sourceFileName.contains("_NORMALS")) {
					targetDir = normalsDir;
					sourceFileName = sourceFileName.replace("_NORMALS", "");
				}

				Path targetFile = targetDir.resolve(sourceFileName);
				if (Files.exists(targetFile)) {
					Logger.debug("Overwriting " + sourceFile);
					Files.delete(targetFile);
				}
				Files.copy(sourceFile, targetFile);
			}
		}

	}

	@Override
	public void write(Path assetsDir) throws IOException {
		Files.copy(packJsonPath, diffuseDir.resolve("pack.json"));
		Files.copy(packJsonPath, normalsDir.resolve("pack.json"));
		copyPlaceholderImages(assetsDir);

		switch (definition.outputFileType) {
			case PACKR_ATLAS_PLUS_NORMALS:
				TexturePacker.process(diffuseDir.toFile().getAbsolutePath(), assetsDir.resolve(definition.assetDir).toFile().getAbsolutePath(), "diffuse-"+definition.outputFileName);
				TexturePacker.process(normalsDir.toFile().getAbsolutePath(), assetsDir.resolve(definition.assetDir).toFile().getAbsolutePath(), "normal-"+definition.outputFileName);
				break;
			case PACKR_ATLAS:
				TexturePacker.process(diffuseDir.toFile().getAbsolutePath(), assetsDir.resolve(definition.assetDir).toFile().getAbsolutePath(), definition.outputFileName);
				break;
			default:
				throw new RuntimeException("Not yet implemented: " + definition.outputFileType + " for " + getClass().getSimpleName());
		}
	}

	private Path createTempSubDir(Path tempDir, String name) throws IOException {
		Path subdir = tempDir.resolve(definition.outputFileName).resolve(name);
		if (Files.exists(subdir)) {
			throw new RuntimeException(subdir + " should not exist");
		}
		Files.createDirectories(subdir);
		return subdir;
	}

	private void copyPlaceholderImages(Path assetsDir) throws IOException {
		// Also copy placeholder images to package
		Path placeholdersDir = assetsDir.resolve("unmoddable");
		Files.copy(placeholdersDir.resolve("placeholder.png"), diffuseDir.resolve("placeholder.png"), REPLACE_EXISTING);
		Files.copy(placeholdersDir.resolve("placeholder_NORMALS.png"), normalsDir.resolve("placeholder.png"), REPLACE_EXISTING);
	}
}
