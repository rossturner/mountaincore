package technology.rocketjump.mountaincore.assets.entities.creature;

import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.jobs.model.Skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByType {

	private Map<EntityAssetType, CreatureEntityAssetsByRace> typeMap = new HashMap<>();

	public void add(CreatureEntityAsset asset) {
		typeMap.computeIfAbsent(asset.getType(), a -> new CreatureEntityAssetsByRace()).add(asset);
	}

	public CreatureEntityAsset get(EntityAssetType type, CreatureEntityAttributes attributes, Skill primaryProfession) {
		return typeMap.get(type).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(EntityAssetType type, CreatureEntityAttributes attributes, Skill primaryProfession) {
		return typeMap.get(type).getAll(attributes, primaryProfession);
	}

}
