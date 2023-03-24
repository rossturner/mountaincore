package technology.rocketjump.mountaincore.assets.entities.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetsByFurnitureType {

	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private Map<FurnitureType, FurnitureEntityAssetsByLayout> byFurnitureType = new HashMap<>();

	public FurnitureEntityAssetsByFurnitureType(FurnitureTypeDictionary furnitureTypeDictionary) {
		this.furnitureTypeDictionary = furnitureTypeDictionary;
	}

	public void add(FurnitureEntityAsset asset) {
		String furnitureTypeName = asset.getFurnitureTypeName();
		FurnitureType furnitureType = furnitureTypeDictionary.getByName(furnitureTypeName);
		if (furnitureType == null) {
			Logger.error("Unrecognised furniture type name in " + asset.getUniqueName() + ": " + furnitureTypeName);
		} else {
			byFurnitureType.computeIfAbsent(furnitureType, a -> new FurnitureEntityAssetsByLayout()).add(asset);
		}
	}

	public FurnitureEntityAsset get(FurnitureEntityAttributes attributes) {
		FurnitureEntityAssetsByLayout childMap = byFurnitureType.get(attributes.getFurnitureType());
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<FurnitureEntityAsset> getAll(FurnitureEntityAttributes attributes) {
		FurnitureEntityAssetsByLayout childMap = byFurnitureType.get(attributes.getFurnitureType());
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
