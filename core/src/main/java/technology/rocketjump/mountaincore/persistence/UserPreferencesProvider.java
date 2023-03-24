package technology.rocketjump.mountaincore.persistence;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;

/**
 * This class is required to orchestrate the initialisation across two dependent classes (once)
 */
@Singleton
public class UserPreferencesProvider implements Provider<UserPreferences> {

	private final UserFileManager userFileManager;
	private final UserPreferences userPreferences;

	@Inject
	public UserPreferencesProvider(UserFileManager userFileManager) {
		this.userFileManager = userFileManager;

		userPreferences = new UserPreferences(userFileManager);
		if (userPreferences.getPreference(UserPreferences.PreferenceKey.SAVE_LOCATION) == null) {
			userPreferences.setPreference(UserPreferences.PreferenceKey.SAVE_LOCATION,
					userFileManager.userFileDirectoryForGame.getAbsolutePath() + File.separator + "saves");
		}
		try {
			userFileManager.initSaveDir(userPreferences.getPreference(UserPreferences.PreferenceKey.SAVE_LOCATION));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public UserPreferences get() {
		return userPreferences;
	}
}
