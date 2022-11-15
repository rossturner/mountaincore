package technology.rocketjump.saul.assets.entities.mechanism;

import technology.rocketjump.saul.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MechanismEntityAssetsByAssetType {

	private final MechanismTypeDictionary mechanismTypeDictionary;
	private Map<EntityAssetType, MechanismEntityAssetsByMechanismType> typeMap = new HashMap<>();

	public MechanismEntityAssetsByAssetType(MechanismTypeDictionary mechanismTypeDictionary) {
		this.mechanismTypeDictionary = mechanismTypeDictionary;
	}

	public void add(MechanismEntityAsset asset) {
		typeMap.computeIfAbsent(asset.getType(), a -> new MechanismEntityAssetsByMechanismType())
				.add(mechanismTypeDictionary.getByName(asset.getMechanismTypeName()), asset);
	}

	public MechanismEntityAsset get(EntityAssetType entityAssetType, MechanismEntityAttributes attributes) {
		MechanismEntityAssetsByMechanismType childMap = typeMap.get(entityAssetType);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<MechanismEntityAsset> getAll(EntityAssetType entityAssetType, MechanismEntityAttributes attributes) {
		MechanismEntityAssetsByMechanismType childMap = typeMap.get(entityAssetType);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
