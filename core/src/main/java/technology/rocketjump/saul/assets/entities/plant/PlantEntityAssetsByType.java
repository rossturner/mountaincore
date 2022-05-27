package technology.rocketjump.saul.assets.entities.plant;

import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantEntityAssetsByType {

	private Map<EntityAssetType, PlantEntityAssetsByGrowthStage> typeMap = new HashMap<>();

	public PlantEntityAssetsByType(EntityAssetTypeDictionary typeDictionary, PlantSpeciesDictionary plantSpeciesDictionary) {
		for (EntityAssetType assetType : typeDictionary.getByEntityType(EntityType.PLANT)) {
			typeMap.put(assetType, new PlantEntityAssetsByGrowthStage(plantSpeciesDictionary));
		}
	}

	public void add(PlantEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.get(asset.getType()).add(asset);
	}

	public PlantEntityAsset get(EntityAssetType type, PlantEntityAttributes attributes) {
		return typeMap.get(type).get(attributes);
	}

	public List<PlantEntityAsset> getAll(EntityAssetType type, PlantEntityAttributes attributes) {
		return typeMap.get(type).getAll(attributes);
	}

}
