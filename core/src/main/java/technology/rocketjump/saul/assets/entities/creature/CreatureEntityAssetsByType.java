package technology.rocketjump.saul.assets.entities.creature;

import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.jobs.model.Skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByType {

	private Map<EntityAssetType, CreatureEntityAssetsByRace> typeMap = new HashMap<>();

	public CreatureEntityAssetsByType(EntityAssetTypeDictionary typeDictionary, RaceDictionary raceDictionary) {
		for (EntityAssetType assetType : typeDictionary.getAll()) {
			typeMap.put(assetType, new CreatureEntityAssetsByRace(raceDictionary));
		}
	}

	public void add(CreatureEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.get(asset.getType()).add(asset);
	}

	public CreatureEntityAsset get(EntityAssetType type, CreatureEntityAttributes attributes, Skill primaryProfession) {
		return typeMap.get(type).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(EntityAssetType type, CreatureEntityAttributes attributes, Skill primaryProfession) {
		return typeMap.get(type).getAll(attributes, primaryProfession);
	}

}
