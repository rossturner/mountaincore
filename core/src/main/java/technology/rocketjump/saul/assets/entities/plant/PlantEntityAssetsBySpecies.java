package technology.rocketjump.saul.assets.entities.plant;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantEntityAssetsBySpecies {

	private final PlantSpeciesDictionary speciesDictionary;
	private Map<PlantSpecies, List<PlantEntityAsset>> speciesMap = new HashMap<>();

	public static final PlantEntityAsset NULL_ENTITY_ASSET;

	static {
		NULL_ENTITY_ASSET = new PlantEntityAsset();
		NULL_ENTITY_ASSET.setType(null);
		NULL_ENTITY_ASSET.setUniqueName("Null entity asset");
		NULL_ENTITY_ASSET.setSpeciesName("None");

	}

	public PlantEntityAssetsBySpecies(PlantSpeciesDictionary speciesDictionary) {
		this.speciesDictionary = speciesDictionary;
	}

	public void add(PlantEntityAsset asset) {
		if (asset.getSpeciesName() != null) {
			// Specific species only
			PlantSpecies plantSpecies = speciesDictionary.getByName(asset.getSpeciesName());
			speciesMap.computeIfAbsent(plantSpecies, a -> new ArrayList<>()).add(asset);
		} else {
			Logger.error(asset.getUniqueName() + " does not have a " + PlantSpecies.class.getSimpleName() + " specified and will not be used");
		}
	}

	public PlantEntityAsset get(PlantEntityAttributes attributes) {
		PlantSpecies plantSpecies = attributes.getSpecies();
		List<PlantEntityAsset> entityAssets = speciesMap.get(plantSpecies);
		if (entityAssets == null) {
			return NULL_ENTITY_ASSET;
		}
		int numAssets = entityAssets.size();
		if (numAssets == 0) {
			return NULL_ENTITY_ASSET;
		} else {
			return entityAssets.get((Math.abs((int)attributes.getSeed())) % numAssets);
		}
	}

	public List<PlantEntityAsset> getAll(PlantEntityAttributes attributes) {
		PlantSpecies species = attributes.getSpecies();
		return speciesMap.getOrDefault(species, List.of());
	}

}
