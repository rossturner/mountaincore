package technology.rocketjump.mountaincore.assets.editor.model;

import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

import java.util.StringJoiner;

public class FurnitureNameBuilders {
    public static String buildUniqueNameForAsset(FurnitureType furnitureType, FurnitureEntityAsset asset) {
        EntityAssetType assetType = asset.getType();
        String layoutNamePlaceholder = "###originallayoutname###";

        StringJoiner uniqueNameJoiner = new StringJoiner("-");
        uniqueNameJoiner.add(furnitureType.getName());
        uniqueNameJoiner.add(layoutNamePlaceholder);
        if (assetType != null) {
            uniqueNameJoiner.add(assetType.getName());
        }
        for (GameMaterialType validMaterialType : asset.getValidMaterialTypes()) {
            uniqueNameJoiner.add(validMaterialType.name());
        }
        String furnitureLayoutName = asset.getFurnitureLayoutName();
        if (furnitureLayoutName == null) {
            furnitureLayoutName = "";
        }

        return WordUtils.capitalizeFully(uniqueNameJoiner.toString(), '_', '-').replace(layoutNamePlaceholder, furnitureLayoutName);

    }
}
