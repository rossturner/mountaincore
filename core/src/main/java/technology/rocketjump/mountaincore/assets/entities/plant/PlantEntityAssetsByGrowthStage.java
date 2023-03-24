package technology.rocketjump.mountaincore.assets.entities.plant;

import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantEntityAssetsByGrowthStage {

	private Map<Integer, PlantEntityAssetsBySpecies> byGrowthStageIndex = new HashMap<>();
	private final PlantSpeciesDictionary plantSpeciesDictionary;

	public PlantEntityAssetsByGrowthStage(PlantSpeciesDictionary plantSpeciesDictionary) {
		this.plantSpeciesDictionary = plantSpeciesDictionary;
	}

	public void add(PlantEntityAsset asset) {
		if (asset.getGrowthStages().isEmpty()) {
			// Add to all
			Logger.error(asset.getUniqueName() + " must have growth stages specified");
		} else {
			for (Integer growthStage : asset.getGrowthStages()) {
				byGrowthStageIndex.computeIfAbsent(growthStage, a -> new PlantEntityAssetsBySpecies(plantSpeciesDictionary)).add(asset);
			}
		}
	}

	public PlantEntityAsset get(PlantEntityAttributes attributes) {
		PlantEntityAssetsBySpecies bySpecies = byGrowthStageIndex.get(attributes.getGrowthStageCursor());
		if (bySpecies == null) {
			Logger.error("No plant assets for growth stage: " + attributes.getGrowthStageCursor());
			return PlantEntityAssetsBySpecies.NULL_ENTITY_ASSET;
		}
		return bySpecies.get(attributes);
	}

	public List<PlantEntityAsset> getAll(PlantEntityAttributes attributes) {
		PlantEntityAssetsBySpecies bySpecies = byGrowthStageIndex.get(attributes.getGrowthStageCursor());
		if (bySpecies == null) {
			Logger.error("No list of plant assets for height: " + attributes.getGrowthStageCursor());
			return List.of();
		}
		return bySpecies.getAll(attributes);
	}
}
