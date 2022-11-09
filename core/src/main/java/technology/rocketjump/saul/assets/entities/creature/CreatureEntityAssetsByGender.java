package technology.rocketjump.saul.assets.entities.creature;

import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.jobs.model.Skill;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;

public class CreatureEntityAssetsByGender {

	private Map<Gender, CreatureEntityAssetsByProfession> genderMap = new EnumMap<>(Gender.class);

	public CreatureEntityAssetsByGender() {
		genderMap.put(Gender.ANY, new CreatureEntityAssetsByProfession());
	}

	public void add(CreatureEntityAsset asset) {
		Gender assetGender = asset.getGender();
		if (assetGender == null || assetGender.equals(Gender.ANY)) {
			// Any gender, add to all lists
			for (Gender racewGender : asset.getRace().getGenders().keySet()) {
				genderMap.computeIfAbsent(racewGender, a -> new CreatureEntityAssetsByProfession()).add(asset);
			}
			genderMap.get(Gender.ANY).add(asset);
		} else {
			// Specific gender only
			genderMap.computeIfAbsent(assetGender, a -> new CreatureEntityAssetsByProfession()).add(asset);
			genderMap.get(Gender.ANY).add(asset);
		}
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Skill primaryProfession) {
		Gender entityGender = attributes.getGender();
		if (entityGender == null) {
			entityGender = Gender.ANY;
		}
		CreatureEntityAssetsByProfession childMap = genderMap.get(entityGender);
		return childMap != null ? childMap.get(attributes, primaryProfession) : NULL_ENTITY_ASSET;
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Skill primaryProfession) {
		Gender entityGender = attributes.getGender();
		if (entityGender == null) {
			entityGender = Gender.ANY;
		}
		CreatureEntityAssetsByProfession childMap = genderMap.get(entityGender);
		return childMap != null ? childMap.getAll(attributes, primaryProfession) : List.of();
	}

}
