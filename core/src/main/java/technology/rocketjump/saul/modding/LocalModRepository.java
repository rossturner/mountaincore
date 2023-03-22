package technology.rocketjump.saul.modding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.AssetsPackager;
import technology.rocketjump.saul.modding.model.ModInfo;
import technology.rocketjump.saul.modding.model.ModioMetadata;
import technology.rocketjump.saul.modding.model.ParsedMod;
import technology.rocketjump.saul.modding.model.WrappedModioJson;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.modding.ModCompatibilityChecker.Compatibility.INCOMPATIBLE;
import static technology.rocketjump.saul.modding.ModParser.MODIO_JSON_FILENAME;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.ACTIVE_MODS;

@Singleton
public class LocalModRepository {

	public static final String MOD_NAME_SEPARATOR = "/";
	public static final List<String> DEFAULT_ACTIVE_MODS = Arrays.asList("base", "Community Translations");
	public static final Path ASSETS_DIR = Paths.get("assets");
	private final ModParser modParser;
	private final ModCompatibilityChecker modCompatibilityChecker;
	private final UserPreferences userPreferences;
	private final AssetsPackager assetsPackager;
	private String originalActiveModString;

	private Map<String, ParsedMod> modsByName = new HashMap<>();
	private List<ParsedMod> activeMods = new ArrayList<>();
	private List<ParsedMod> incompatibleMods = new ArrayList<>();
	private Map<Long, ParsedMod> byModioId = new HashMap<>();
	private boolean changesToApply;
	private ObjectMapper objectMapper = new ObjectMapper();

	@Inject
	public LocalModRepository(ModParser modParser, ModCompatibilityChecker modCompatibilityChecker,
							  UserPreferences userPreferences, AssetsPackager assetsPackager) {
		this.modParser = modParser;
		this.modCompatibilityChecker = modCompatibilityChecker;
		this.userPreferences = userPreferences;
		this.assetsPackager = assetsPackager;

		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		if (!Files.exists(modsDir())) {
			throw new RuntimeException("Can not find 'mods' directory");
		}

		updateLocalModListing();
	}

	public void updateLocalModListing() {
		modsByName.clear();
		activeMods.clear();
		incompatibleMods.clear();
		byModioId.clear();

		try {
			Files.list(modsDir()).forEach(modDirFile -> {
				try {
					if (modDirFile.getFileName().toString().endsWith(".zip")) {
						Path unzippedMod = attemptUnzip(modDirFile);
						if (unzippedMod != null && Files.exists(unzippedMod)) {
							parseModDir(unzippedMod);
							Files.delete(modDirFile);
						}
					}
					if (Files.exists(modDirFile.resolve("modInfo.json"))) {
						parseModDir(modDirFile);
					}
				} catch (Exception e) {
					Logger.error("Error while parsing mod from " + modDirFile, e);
				}
			});
		} catch (IOException e) {
			Logger.error(e.getMessage());
		}

		String activeModsString = userPreferences.getPreference(ACTIVE_MODS);
		this.originalActiveModString = activeModsString;
		for (String activeModName : activeModsString.split(MOD_NAME_SEPARATOR)) {
			ParsedMod activeMod = modsByName.get(activeModName);
			if (activeMod != null) {
				if (modCompatibilityChecker.checkCompatibility(activeMod).compatibility().equals(INCOMPATIBLE)) {
					Logger.warn(activeMod.getInfo().toString() + " is not compatible with this game version ("+GlobalSettings.VERSION +")");
					incompatibleMods.add(activeMod);
				} else {
					activeMods.add(activeMod);
				}
			} else {
				Logger.error("Missing mod with name: " + activeModName);
			}
		}
	}

	public Path attemptUnzip(Path zipFile) throws IOException, ArchiveException {
		Path tempDir = modsDir().resolve("temp");
		if (Files.exists(tempDir)) {
			Files.delete(tempDir);
		}
		Files.createDirectory(tempDir);
		try {
			tempDir.toFile().setWritable(true);
			try (InputStream inputStream = Files.newInputStream(zipFile)) {
				ArchiveInputStream archiveInput = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, inputStream);
				ArchiveEntry entry = archiveInput.getNextEntry();
				while (entry != null) {
					if (!archiveInput.canReadEntryData(entry)) {
						Logger.error("Can not read entry data for " + entry.getName() + " in " + zipFile);
						entry = archiveInput.getNextEntry();
						continue;
					}

					Path targetPath = tempDir.resolve(entry.getName());
					if (entry.isDirectory()) {
						Files.createDirectory(targetPath);
						targetPath.toFile().setWritable(true);
					} else {
						Files.createDirectories(targetPath.getParent());

						try (OutputStream out = Files.newOutputStream(targetPath)) {
							IOUtils.copy(archiveInput, out);
						}
					}
					entry = archiveInput.getNextEntry();
				}


				Path modBasePath = tempDir;
				List<Path> basePathEntries = Files.list(modBasePath).toList();
				if (basePathEntries.size() == 1 && Files.isDirectory(basePathEntries.get(0))) {
					modBasePath = basePathEntries.get(0);
				}

				if (Files.exists(modBasePath.resolve("modInfo.json"))) {
					// This is a valid mod zip
					ModInfo modInfo = ModParser.readModInfo(modBasePath, objectMapper);

					Path targetPath = modsDir().resolve(modInfo.getNameId());
					if (Files.exists(targetPath)) {
						Files.delete(targetPath);
					}
					Files.move(modBasePath, targetPath);
					return targetPath;
				}
			}
		} finally {
			if (Files.exists(tempDir)) {
				Files.delete(tempDir);
			}
		}
		return null;
	}

	private void parseModDir(Path modDirFile) throws IOException {
		ParsedMod parsedMod = modParser.parseMod(modDirFile);
		modsByName.put(parsedMod.getInfo().getName(), parsedMod);
		if (parsedMod.getModioMeta().isPresent()) {
			byModioId.put(parsedMod.getModioMeta().get().getModioId(), parsedMod);
		}
	}

	public void setActiveMods(List<ParsedMod> activeMods) {
		this.activeMods = activeMods;
		String preferenceString = this.activeMods.stream().map(m -> m.getInfo().getName()).collect(Collectors.joining(MOD_NAME_SEPARATOR));
		userPreferences.setPreference(ACTIVE_MODS, preferenceString);
		Logger.info("Set ACTIVE_MODS to " + preferenceString);

		this.changesToApply = !preferenceString.equals(originalActiveModString);
	}

	public void delete(ParsedMod localMod, boolean removeFromActiveMods) throws IOException {
		FileUtils.deleteDirectory(localMod.getBasePath().toFile());
		modsByName.remove(localMod.getInfo().getName());
		if (removeFromActiveMods) {
			activeMods.remove(localMod);
		}
		incompatibleMods.remove(localMod);
		localMod.getModioMeta().ifPresent(meta -> byModioId.remove(meta.getModioId()));
	}

	public boolean hasChangesToApply() {
		return changesToApply;
	}

	public void packageActiveMods() {
		assetsPackager.packageModsToAssets(activeMods, ASSETS_DIR);
	}

	public Collection<ParsedMod> getAll() {
		return modsByName.values();
	}

	public List<ParsedMod> getActiveMods() {
		return activeMods;
	}

	public List<ParsedMod> getIncompatibleMods() {
		return incompatibleMods;
	}

	public void writeDownload(InputStream downloadStream, WrappedModioJson mod) throws IOException, ArchiveException {
		Path targetPath = modsDir().resolve(mod.getFilename());
		Files.deleteIfExists(targetPath);
		Files.createFile(targetPath);
		targetPath.toFile().setWritable(true);

		try (OutputStream outputStream = Files.newOutputStream(targetPath)) {
			IOUtils.copy(downloadStream, outputStream, 4096);
		}

		Path unzippedMod = attemptUnzip(targetPath);
		if (unzippedMod != null && Files.exists(unzippedMod)) {
			createModioMeta(unzippedMod, mod);
			parseModDir(unzippedMod);
			Files.delete(targetPath);
		}
	}

	private void createModioMeta(Path unzippedMod, WrappedModioJson mod) throws IOException {
		Path modioPath = unzippedMod.resolve(MODIO_JSON_FILENAME);
		Files.deleteIfExists(modioPath);
		Files.createFile(modioPath);
		modioPath.toFile().setWritable(true);

		ModioMetadata modioMetadata = new ModioMetadata();
		modioMetadata.setModioId(mod.getModioId());
		modioMetadata.setDownloadChecksum(mod.getModfileHash());

		String fileContents = objectMapper.writeValueAsString(modioMetadata);
		Files.writeString(modioPath, fileContents);
	}

	public ParsedMod getByModioId(long id) {
		return byModioId.get(id);
	}

	private static Path modsDir() {
		return Paths.get("mods");
	}
}
