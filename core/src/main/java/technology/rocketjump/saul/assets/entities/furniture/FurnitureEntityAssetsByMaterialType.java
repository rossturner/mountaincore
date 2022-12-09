package technology.rocketjump.saul.assets.entities.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetsByMaterialType {

	private Map<GameMaterialType, List<FurnitureEntityAsset>> byMaterialType = new EnumMap<>(GameMaterialType.class);

	public void add(FurnitureEntityAsset asset) {
		List<GameMaterialType> materialTypes = asset.getValidMaterialTypes();
		if (materialTypes == null) {
			throw new RuntimeException("Material types must be specified for " + asset);
		} else {
			for (GameMaterialType materialType : materialTypes) {
				byMaterialType.computeIfAbsent(materialType, a -> new ArrayList<>()).add(asset);
			}
		}
	}

	public FurnitureEntityAsset get(FurnitureEntityAttributes attributes) {
		List<FurnitureEntityAsset> assets = byMaterialType.getOrDefault(attributes.getPrimaryMaterialType(), List.of());
		if (assets.size() == 0) {
			Logger.error("Could not find applicable asset for " + attributes);
			return null;
		} else {
			return assets.get((Math.abs((int)attributes.getSeed())) % assets.size());
		}

	}

	public List<FurnitureEntityAsset> getAll(FurnitureEntityAttributes attributes) {
		return byMaterialType.getOrDefault(attributes.getPrimaryMaterialType(), List.of());
	}

}
