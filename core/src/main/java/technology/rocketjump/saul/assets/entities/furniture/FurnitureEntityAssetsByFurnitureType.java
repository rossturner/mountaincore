package technology.rocketjump.saul.assets.entities.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetsByFurnitureType {

	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private Map<FurnitureType, FurnitureEntityAssetsByLayout> byFurnitureType = new HashMap<>();

	public FurnitureEntityAssetsByFurnitureType(FurnitureTypeDictionary furnitureTypeDictionary, FurnitureLayoutDictionary layoutDictionary) {
		for (FurnitureType furnitureType : furnitureTypeDictionary.getAll()) {
			byFurnitureType.put(furnitureType, new FurnitureEntityAssetsByLayout(layoutDictionary));
		}
		this.furnitureTypeDictionary = furnitureTypeDictionary;
	}

	public void add(FurnitureEntityAsset asset) {
		add(asset.getFurnitureTypeName(), asset);
	}

	private void add(String furnitureTypeName, FurnitureEntityAsset asset) {
		FurnitureType furnitureType = furnitureTypeDictionary.getByName(furnitureTypeName);
		if (furnitureType == null) {
			Logger.error("Unrecognised furniture type name in " + asset.getUniqueName() + ": " + furnitureTypeName);
		} else {
			byFurnitureType.get(furnitureType).add(asset);
		}
	}

	public FurnitureEntityAsset get(FurnitureEntityAttributes attributes) {
		return byFurnitureType.get(attributes.getFurnitureType()).get(attributes);
	}

	public List<FurnitureEntityAsset> getAll(FurnitureEntityAttributes attributes) {
		return byFurnitureType.get(attributes.getFurnitureType()).getAll(attributes);
	}

}
