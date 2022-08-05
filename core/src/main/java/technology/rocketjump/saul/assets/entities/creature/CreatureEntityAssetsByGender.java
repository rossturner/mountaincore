package technology.rocketjump.saul.assets.entities.creature;

import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.jobs.model.Skill;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByGender {

	private Map<Gender, CreatureEntityAssetsByProfession> genderMap = new EnumMap<>(Gender.class);

	public CreatureEntityAssetsByGender(Race race) {
		for (Gender type : race.getGenders().keySet()) {
			genderMap.put(type, new CreatureEntityAssetsByProfession());
		}
		genderMap.put(Gender.ANY, new CreatureEntityAssetsByProfession());
		genderMap.put(Gender.NONE, new CreatureEntityAssetsByProfession());
	}

	public void add(CreatureEntityAsset asset) {
		Gender gender = asset.getGender();
		if (gender == null || gender.equals(Gender.ANY)) {
			// Any gender, add to all lists
			for (CreatureEntityAssetsByProfession assetsByProfession : genderMap.values()) {
				assetsByProfession.add(asset);
			}
		} else {
			// Specific gender only
			genderMap.computeIfAbsent(gender, a -> new CreatureEntityAssetsByProfession()).add(asset);
			genderMap.get(Gender.ANY).add(asset);
		}
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Skill primaryProfession) {
		Gender entityGender = attributes.getGender();
		if (entityGender == null) {
			entityGender = Gender.ANY;
		}
		return genderMap.get(entityGender).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Skill primaryProfession) {
		Gender entityGender = attributes.getGender();
		if (entityGender == null) {
			entityGender = Gender.ANY;
		}
		return genderMap.get(entityGender).getAll(attributes, primaryProfession);
	}

}
