package technology.rocketjump.mountaincore.persistence.migrations;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.RandomXS128;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.misc.versioning.Version;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;

import java.util.List;
import java.util.Random;

public class EA_1_2_5 implements SavedGameMigration {

    private static final Version VERSION = new Version("Early Access 1.2.5");
    private final Random random = new RandomXS128();

    @Override
    public Version getVersionApplicableTo() {
        return VERSION;
    }

    /**
     * Removes materials:
     * Wolframite, Thorium, Pollucite, Ilmenite
     *
     * @return
     */
    @Override
    public JSONObject apply(JSONObject saveFileJson, SavedGameDependentDictionaries relatedStores) {
        String rawJsonString = saveFileJson.toJSONString();

        List<GameMaterial> otherOres = relatedStores.gameMaterialDictionary.getByType(GameMaterialType.ORE);
        for (String removedMaterialName : List.of("Wolframite", "Thorium", "Pollucite", "Ilmenite")) {
            GameMaterial replacementMaterial = otherOres.get(random.nextInt(otherOres.size()));
            rawJsonString = StringUtils.replace(rawJsonString, "\"" + removedMaterialName + "\"", "\"" + replacementMaterial.getMaterialName() + "\"");
        }

        return JSON.parseObject(rawJsonString);
    }
}
