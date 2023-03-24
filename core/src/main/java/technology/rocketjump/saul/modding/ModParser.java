package technology.rocketjump.saul.modding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.modding.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Singleton
public class ModParser {

	public static final String MOD_INFO_FILENAME = "modInfo.json";
	public static final String MODIO_JSON_FILENAME = "modio.json";
	private final ModArtifactListing artifactListing;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Inject
	public ModParser(ModArtifactListing modArtifactListing) {
		this.artifactListing = modArtifactListing;
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public ParsedMod parseMod(Path modBasePath) throws IOException {
		ParsedMod parsedMod = new ParsedMod(modBasePath, readModInfo(modBasePath, objectMapper), parseModioMeta(modBasePath));

		for (ModArtifactDefinition artifactDefinition : artifactListing.getAll()) {
			Optional<ModArtifact> artifact = new ArtifactParser(artifactDefinition).parse(modBasePath);
			artifact.ifPresent(parsedMod::add);
		}

		return parsedMod;
	}

	public static ModInfo readModInfo(Path modBasePath, ObjectMapper objectMapper) throws IOException {
		Path infoPath = modBasePath.resolve(MOD_INFO_FILENAME);
		if (!Files.exists(infoPath)) {
			throw new IOException("Could not find modInfo.json in " + modBasePath);
		}
		return objectMapper.readValue(FileUtils.readFileToString(infoPath.toFile(), "UTF-8"), ModInfo.class);
	}

	private Optional<ModioMetadata> parseModioMeta(Path modBasePath) {
		Path modioMetaPath = modBasePath.resolve(MODIO_JSON_FILENAME);
		if (Files.exists(modioMetaPath)) {
			try {
				return Optional.of(objectMapper.readValue(FileUtils.readFileToString(modioMetaPath.toFile(), "UTF-8"), ModioMetadata.class));
			} catch (IOException e) {
				Logger.error("Error parsing modio.json for " + modBasePath, e);
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	public void write(Path directory, ModInfo modInfo) throws IOException {
		Path modInfoFile = directory.resolve(MOD_INFO_FILENAME);
		objectMapper.writeValue(modInfoFile.toFile(), modInfo);
	}

	public ModArtifactListing getArtifactListing() {
		return artifactListing;
	}

}
