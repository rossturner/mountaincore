package technology.rocketjump.mountaincore.persistence;

import com.alibaba.fastjson.JSONObject;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.mountaincore.misc.versioning.Version;
import technology.rocketjump.mountaincore.persistence.migrations.SavedGameMigration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Singleton
public class SavedGameMigrationService {

    private final List<SavedGameMigration> migrations = new ArrayList<>();
    private final SavedGameDependentDictionaries relatedStores;

    @Inject
    public SavedGameMigrationService(SavedGameDependentDictionaries relatedStores) throws
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.relatedStores = relatedStores;

        Reflections reflections = new Reflections("technology.rocketjump.mountaincore.persistence.migrations", new SubTypesScanner());
        Set<Class<? extends SavedGameMigration>> migrationClasses = reflections.getSubTypesOf(SavedGameMigration.class);
        for (Class<? extends SavedGameMigration> migrationClass : migrationClasses) {
            migrations.add(migrationClass.getDeclaredConstructor().newInstance());
        }

        if (migrations.isEmpty()) {
            throw new IllegalStateException("No migrations found");
        }

        migrations.sort(Comparator.comparing(SavedGameMigration::getVersionApplicableTo));
    }

    public JSONObject migrate(SavedGameInfo savedGameInfo, JSONObject saveFileJson) {
        Version saveVersion = new Version(savedGameInfo.version);
        for (SavedGameMigration migration : migrations) {
            if (migration.getVersionApplicableTo().toInteger() >= saveVersion.toInteger()) {
                saveFileJson = migration.apply(saveFileJson, relatedStores);
            }
        }
        return saveFileJson;
    }
}
