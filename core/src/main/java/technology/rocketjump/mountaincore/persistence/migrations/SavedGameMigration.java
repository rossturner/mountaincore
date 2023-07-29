package technology.rocketjump.mountaincore.persistence.migrations;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.misc.versioning.Version;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;

/**
 * Defines a migration to be applied to a saved game to fix any compatibility-breaking changes in a new version
 *
 * Note that the migration version should match the version of the most previously *released* version of the game,
 * or in other words *not* be the same version as the build about to be released, so that these migrations only act once,
 * and then the game is saved with the latest version.
 */
public interface SavedGameMigration {

    Version getVersionApplicableTo();

    JSONObject apply(JSONObject saveFileJson, SavedGameDependentDictionaries relatedStores);

}
