package technology.rocketjump.mountaincore.assets.entities.creature;

import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.jobs.model.Skill;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;

public class CreatureEntityAssetsByBodyType {

	private Map<CreatureBodyShape, CreatureEntityAssetsByGender> bodyTypeMap = new EnumMap<>(CreatureBodyShape.class);

	public CreatureEntityAssetsByBodyType() {
		bodyTypeMap.put(CreatureBodyShape.ANY, new CreatureEntityAssetsByGender());
	}

	public void add(CreatureEntityAsset asset) {
		CreatureBodyShape assetBodyShape = asset.getBodyShape();
		if (assetBodyShape == null) {
			// Any body type, add to all lists for this species
			for (CreatureBodyShapeDescriptor bodyShape : asset.getRace().getBodyShapes()) {
				bodyTypeMap.computeIfAbsent(bodyShape.getValue(), a -> new CreatureEntityAssetsByGender()).add(asset);
			}
			bodyTypeMap.get(CreatureBodyShape.ANY).add(asset);
		} else {
			// Specific bodyShape only
			bodyTypeMap.computeIfAbsent(assetBodyShape, a -> new CreatureEntityAssetsByGender()).add(asset);
			bodyTypeMap.get(CreatureBodyShape.ANY).add(asset);
		}
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Skill primaryProfession) {
		CreatureBodyShape bodyType = attributes.getBodyShape();
		if (bodyType == null) {
			bodyType = CreatureBodyShape.ANY;
		}
		CreatureEntityAssetsByGender childMap = bodyTypeMap.get(bodyType);
		return childMap != null ? childMap.get(attributes, primaryProfession) : NULL_ENTITY_ASSET;
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Skill primaryProfession) {
		CreatureBodyShape bodyType = attributes.getBodyShape();
		if (bodyType == null) {
			bodyType = CreatureBodyShape.ANY;
		}
		CreatureEntityAssetsByGender childMap = bodyTypeMap.get(bodyType);
		return childMap != null ? childMap.getAll(attributes, primaryProfession) : List.of();
	}
}
