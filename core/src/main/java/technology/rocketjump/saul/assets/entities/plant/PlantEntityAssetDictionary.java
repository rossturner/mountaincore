package technology.rocketjump.saul.assets.entities.plant;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.assets.entities.plant.PlantEntityAssetsBySpecies.NULL_ENTITY_ASSET;

/**
 * This class stores all the entity assets for use in the game,
 * either for rendering or attaching to entities at creation
 */
@ProvidedBy(PlantEntityAssetDictionaryProvider.class)
@Singleton
public class PlantEntityAssetDictionary {

	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private PlantEntityAssetsByType typeMap;
	private final Map<String, PlantEntityAsset> assetsByName = new HashMap<>();

	public PlantEntityAssetDictionary(List<PlantEntityAsset> completeAssetList, EntityAssetTypeDictionary entityAssetTypeDictionary,
									  PlantSpeciesDictionary plantSpeciesDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		for (PlantEntityAsset asset : completeAssetList) {
			assetsByName.put(asset.getUniqueName(), asset);
		}
		rebuild();
		assetsByName.put(NULL_ENTITY_ASSET.getUniqueName(), NULL_ENTITY_ASSET);
	}

	public PlantEntityAsset getByUniqueName(String uniqueAssetName) {
		PlantEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return NULL_ENTITY_ASSET;
		}
	}

	public PlantEntityAsset getPlantEntityAsset(EntityAssetType assetType, PlantEntityAttributes attributes) {
		return typeMap.get(assetType, attributes);
	}

	public List<PlantEntityAsset> getAllMatchingAssets(EntityAssetType assetType, PlantEntityAttributes attributes) {
		return typeMap.getAll(assetType, attributes);
	}

	public Map<String, PlantEntityAsset> getAll() {
		return assetsByName;
	}

	public void rebuild() {
		this.typeMap = new PlantEntityAssetsByType(entityAssetTypeDictionary, plantSpeciesDictionary);
		for (PlantEntityAsset asset : assetsByName.values()) {
			if (NULL_ENTITY_ASSET != asset) {
				typeMap.add(asset);
			}
		}
	}

	public void add(PlantEntityAsset asset) {
		assetsByName.put(asset.getUniqueName(), asset);
		rebuild();
	}
}
