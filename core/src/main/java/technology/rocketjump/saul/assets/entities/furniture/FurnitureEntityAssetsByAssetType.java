package technology.rocketjump.saul.assets.entities.furniture;

import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetsByAssetType {

	private final Map<EntityAssetType, FurnitureEntityAssetsByFurnitureType> byAssetType = new HashMap<>();
	private final FurnitureTypeDictionary furnitureTypeDictionary;

	public FurnitureEntityAssetsByAssetType(FurnitureTypeDictionary furnitureTypeDictionary) {
		this.furnitureTypeDictionary = furnitureTypeDictionary;
	}

	public void add(FurnitureEntityAsset asset) {
		byAssetType.computeIfAbsent(asset.getType(), a -> new FurnitureEntityAssetsByFurnitureType(furnitureTypeDictionary)).add(asset);
	}

	public FurnitureEntityAsset get(EntityAssetType entityAssetType, FurnitureEntityAttributes attributes) {
		FurnitureEntityAssetsByFurnitureType childMap = byAssetType.get(entityAssetType);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<FurnitureEntityAsset> getAll(EntityAssetType entityAssetType, FurnitureEntityAttributes attributes) {
		FurnitureEntityAssetsByFurnitureType childMap = byAssetType.get(entityAssetType);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
