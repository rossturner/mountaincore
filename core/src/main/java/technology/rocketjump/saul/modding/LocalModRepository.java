package technology.rocketjump.saul.modding;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.AssetsPackager;
import technology.rocketjump.saul.modding.model.ParsedMod;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.modding.ModCompatibilityChecker.Compatibility.INCOMPATIBLE;
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
	private final Path modsDir;

	private Map<String, ParsedMod> modsByName = new HashMap<>();
	private List<ParsedMod> activeMods = new ArrayList<>();
	private List<ParsedMod> incompatibleMods = new ArrayList<>();
	private boolean changesToApply;

	@Inject
	public LocalModRepository(ModParser modParser, ModCompatibilityChecker modCompatibilityChecker,
							  UserPreferences userPreferences, AssetsPackager assetsPackager) {
		this.modParser = modParser;
		this.modCompatibilityChecker = modCompatibilityChecker;
		this.userPreferences = userPreferences;
		this.assetsPackager = assetsPackager;

		modsDir = Paths.get("mods");
		if (!Files.exists(modsDir)) {
			throw new RuntimeException("Can not find 'mods' directory");
		}

		updateLocalModListing();

	}

	public void updateLocalModListing() {
		modsByName.clear();
		activeMods.clear();
		incompatibleMods.clear();

		try {
			Files.list(modsDir).forEach(modDir -> {
				if (Files.exists(modDir.resolve("modInfo.json"))) {
					try {
						ParsedMod parsedMod = modParser.parseMod(modDir);
						modsByName.put(parsedMod.getInfo().getName(), parsedMod);
					} catch (Exception e) {
						Logger.error("Error while parsing mod from " + modDir, e);
					}
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

	public void setActiveMods(List<ParsedMod> activeMods) {
		this.activeMods = activeMods;
		String preferenceString = this.activeMods.stream().map(m -> m.getInfo().getName()).collect(Collectors.joining(MOD_NAME_SEPARATOR));
		userPreferences.setPreference(ACTIVE_MODS, preferenceString);
		Logger.info("Set ACTIVE_MODS to " + preferenceString);

		this.changesToApply = !preferenceString.equals(originalActiveModString);
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
}
