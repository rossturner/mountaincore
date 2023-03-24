package technology.rocketjump.mountaincore.assets.editor.model;

import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleType;

import java.util.StringJoiner;

public class VehicleNameBuilders {
    public static String buildUniqueNameForAsset(VehicleType vehicleType, VehicleEntityAsset asset) {
        EntityAssetType assetType = asset.getType();

        StringJoiner uniqueNameJoiner = new StringJoiner("-");
        uniqueNameJoiner.add(vehicleType.getName());
        if (assetType != null) {
            uniqueNameJoiner.add(assetType.getName());
        }
        return WordUtils.capitalizeFully(uniqueNameJoiner.toString(), '_', '-');

    }
}
