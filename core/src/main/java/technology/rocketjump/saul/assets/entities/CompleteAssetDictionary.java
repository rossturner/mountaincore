package technology.rocketjump.saul.assets.entities;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.furniture.FurnitureEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.mechanism.MechanismEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.plant.PlantEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.wallcap.WallCapAssetDictionary;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.saul.assets.entities.model.NullEntityAsset.NULL_ASSET;

@Singleton
public class CompleteAssetDictionary {

	private final Map<String, EntityAsset> allAssetsByName = new HashMap<>();

	@Inject
	public CompleteAssetDictionary(CreatureEntityAssetDictionary creatureEntityAssetDictionary, FurnitureEntityAssetDictionary furnitureEntityAssetDictionary,
								   PlantEntityAssetDictionary plantEntityAssetDictionary, ItemEntityAssetDictionary itemEntityAssetDictionary,
								   WallCapAssetDictionary wallCapAssetDictionary, MechanismEntityAssetDictionary mechanismEntityAssetDictionary) {
		allAssetsByName.putAll(creatureEntityAssetDictionary.getAll());
		allAssetsByName.putAll(plantEntityAssetDictionary.getAll());
		allAssetsByName.putAll(itemEntityAssetDictionary.getAll());
		allAssetsByName.putAll(furnitureEntityAssetDictionary.getAll());
		allAssetsByName.putAll(wallCapAssetDictionary.getAll());
		allAssetsByName.putAll(mechanismEntityAssetDictionary.getAll());
		allAssetsByName.put(NULL_ASSET.getUniqueName(), NULL_ASSET);
	}

	public EntityAsset getByUniqueName(String uniqueAssetName) {
		return allAssetsByName.get(uniqueAssetName);
	}


}
