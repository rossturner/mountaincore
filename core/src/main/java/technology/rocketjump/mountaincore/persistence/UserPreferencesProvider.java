package technology.rocketjump.mountaincore.persistence;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static technology.rocketjump.mountaincore.persistence.UserFileManager.GAME_NAME_FOR_FILESYSTEM;

/**
 * This class is required to orchestrate the initialisation across two dependent classes (once)
 */
@Singleton
public class UserPreferencesProvider implements Provider<UserPreferences> {

	private final UserFileManager userFileManager;
	private final UserPreferences userPreferences;

	@Inject
	public UserPreferencesProvider(UserFileManager userFileManager) throws IOException {
		this.userFileManager = userFileManager;

		userPreferences = new UserPreferences(userFileManager);
		if (userPreferences.getPreference(UserPreferences.PreferenceKey.SAVE_LOCATION) == null) {
			userPreferences.setPreference(UserPreferences.PreferenceKey.SAVE_LOCATION, getSaveLocation(userFileManager));
		}
		try {
			userFileManager.initSaveDir(userPreferences.getPreference(UserPreferences.PreferenceKey.SAVE_LOCATION));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getSaveLocation(UserFileManager userFileManager) throws IOException {
		Path systemSavedGamesDir = userFileManager.userFileDirectoryForGame.toPath().resolve("../../Saved Games");
		if (Files.exists(systemSavedGamesDir)) {
			return systemSavedGamesDir.toFile().getCanonicalFile() + File.separator + GAME_NAME_FOR_FILESYSTEM;
		} else {
			return userFileManager.userFileDirectoryForGame.getAbsolutePath() + File.separator + "saves";
		}
	}

	@Override
	public UserPreferences get() {
		return userPreferences;
	}
}
