package technology.rocketjump.mountaincore.assets.entities.plant;

import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantEntityAssetsByType {

	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private final Map<EntityAssetType, PlantEntityAssetsByGrowthStage> typeMap = new HashMap<>();

	public PlantEntityAssetsByType(PlantSpeciesDictionary plantSpeciesDictionary) {
		this.plantSpeciesDictionary = plantSpeciesDictionary;
	}

	public void add(PlantEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.computeIfAbsent(asset.getType(), a -> new PlantEntityAssetsByGrowthStage(plantSpeciesDictionary)).add(asset);
	}

	public PlantEntityAsset get(EntityAssetType type, PlantEntityAttributes attributes) {
		PlantEntityAssetsByGrowthStage childMap = typeMap.get(type);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<PlantEntityAsset> getAll(EntityAssetType type, PlantEntityAttributes attributes) {
		PlantEntityAssetsByGrowthStage childMap = typeMap.get(type);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
