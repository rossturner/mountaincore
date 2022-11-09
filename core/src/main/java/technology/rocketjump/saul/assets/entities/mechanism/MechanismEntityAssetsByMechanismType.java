package technology.rocketjump.saul.assets.entities.mechanism;

import technology.rocketjump.saul.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechanismEntityAssetsByMechanismType {

	private Map<MechanismType, MechanismEntityAssetsByLayout> typeMap = new HashMap<>();

	public void add(MechanismType mechanismType, MechanismEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.computeIfAbsent(mechanismType, a -> new MechanismEntityAssetsByLayout()).add(asset);
	}

	public MechanismEntityAsset get(MechanismEntityAttributes attributes) {
		MechanismEntityAssetsByLayout childMap = typeMap.get(attributes.getMechanismType());
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<MechanismEntityAsset> getAll(MechanismEntityAttributes attributes) {
		MechanismEntityAssetsByLayout childMap = typeMap.get(attributes.getMechanismType());
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
