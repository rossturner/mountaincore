package technology.rocketjump.saul.assets.entities.mechanism;

import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetType.NULL_ENTITY_ASSET_TYPE;

public class MechanismEntityAssetsByAssetType {

	private final MechanismTypeDictionary mechanismTypeDictionary;
	private Map<EntityAssetType, MechanismEntityAssetsByMechanismType> typeMap = new HashMap<>();

	public MechanismEntityAssetsByAssetType(EntityAssetTypeDictionary typeDictionary, MechanismTypeDictionary mechanismTypeDictionary) {
		this.mechanismTypeDictionary = mechanismTypeDictionary;
		for (EntityAssetType assetType : typeDictionary.getByEntityType(EntityType.MECHANISM)) {
			typeMap.put(assetType, new MechanismEntityAssetsByMechanismType(mechanismTypeDictionary));
		}
		typeMap.put(NULL_ENTITY_ASSET_TYPE, new MechanismEntityAssetsByMechanismType(mechanismTypeDictionary));
	}

	public void add(MechanismEntityAsset asset) {
		// Assuming all entities have a type specified
		if (!typeMap.containsKey(asset.getType())) {
			throw new RuntimeException("Unrecognised asset type " + asset.getType() + " for " + asset.getUniqueName());
		}

		typeMap.get(asset.getType()).add(mechanismTypeDictionary.getByName(asset.getMechanismTypeName()), asset);
		typeMap.get(NULL_ENTITY_ASSET_TYPE).add(mechanismTypeDictionary.getByName(asset.getMechanismTypeName()), asset);
	}

	public MechanismEntityAsset get(EntityAssetType entityAssetType, MechanismEntityAttributes attributes) {
		if (entityAssetType == null) {
			entityAssetType = NULL_ENTITY_ASSET_TYPE;
		}
		return typeMap.get(entityAssetType).get(attributes);
	}

	public List<MechanismEntityAsset> getAll(EntityAssetType entityAssetType, MechanismEntityAttributes attributes) {
		return typeMap.get(entityAssetType).getAll(attributes);
	}

}
