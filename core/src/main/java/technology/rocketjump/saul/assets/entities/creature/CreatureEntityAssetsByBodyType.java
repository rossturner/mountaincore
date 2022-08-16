package technology.rocketjump.saul.assets.entities.creature;

import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.jobs.model.Skill;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetsByBodyType {

	private Map<CreatureBodyShape, CreatureEntityAssetsByGender> bodyTypeMap = new EnumMap<>(CreatureBodyShape.class);

	public CreatureEntityAssetsByBodyType(Race race) {
		for (CreatureBodyShapeDescriptor type : race.getBodyShapes()) {
			bodyTypeMap.put(type.getValue(), new CreatureEntityAssetsByGender(race));
		}
		bodyTypeMap.put(CreatureBodyShape.ANY, new CreatureEntityAssetsByGender(race));
	}

	public void add(CreatureEntityAsset asset) {
		CreatureBodyShape bodyType = asset.getBodyShape();
		if (bodyType == null) {
			// Any body type, add to all lists
			for (CreatureEntityAssetsByGender creatureEntityAssetsByGender : bodyTypeMap.values()) {
				creatureEntityAssetsByGender.add(asset);
			}
		} else {
			// Specific bodytype only
			bodyTypeMap.get(bodyType).add(asset);
			bodyTypeMap.get(CreatureBodyShape.ANY).add(asset);
		}
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Skill primaryProfession) {
		CreatureBodyShape bodyType = attributes.getBodyShape();
		if (bodyType == null) {
			bodyType = CreatureBodyShape.ANY;
		}
		return bodyTypeMap.get(bodyType).get(attributes, primaryProfession);
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Skill primaryProfession) {
		CreatureBodyShape bodyType = attributes.getBodyShape();
		if (bodyType == null) {
			bodyType = CreatureBodyShape.ANY;
		}
		return bodyTypeMap.get(bodyType).getAll(attributes, primaryProfession);
	}
}
