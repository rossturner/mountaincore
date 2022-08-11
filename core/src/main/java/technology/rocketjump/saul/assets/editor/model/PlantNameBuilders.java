package technology.rocketjump.saul.assets.editor.model;

import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;

import java.util.StringJoiner;

public class PlantNameBuilders {
    public static String buildUniqueNameForAsset(PlantSpecies plantSpecies, PlantEntityAsset asset) {
        EntityAssetType assetType = asset.getType();

        StringJoiner uniqueNameJoiner = new StringJoiner("-");
        uniqueNameJoiner.add(plantSpecies.getSpeciesName());
        if (assetType != null) {
            uniqueNameJoiner.add(assetType.getName());
        }
        StringJoiner growthStageTermJoiner = new StringJoiner("_");
        for (Integer growthStage : asset.getGrowthStages().stream().sorted().toList()) {
            growthStageTermJoiner.add(growthStage.toString());
        }

        uniqueNameJoiner.add(growthStageTermJoiner.toString());
        return WordUtils.capitalizeFully(uniqueNameJoiner.toString(), '_', '-');

    }
}
