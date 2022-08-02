package technology.rocketjump.saul.assets.entities.creature;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.ProvidedBy;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.jobs.model.Profession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;

/**
 * This class stores all the entity assets for use in the game,
 * either for rendering or attaching to entities at creation
 */
@ProvidedBy(CreatureEntityAssetDictionaryProvider.class)
public class CreatureEntityAssetDictionary {

	private final Map<String, CreatureEntityAsset> assetsByName = new HashMap<>();
	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final RaceDictionary raceDictionary;
	private CreatureEntityAssetsByType typeMap;

	public CreatureEntityAssetDictionary(List<CreatureEntityAsset> completeAssetList, EntityAssetTypeDictionary entityAssetTypeDictionary, RaceDictionary raceDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.raceDictionary = raceDictionary;
		for (CreatureEntityAsset asset : completeAssetList) {
			assetsByName.put(asset.getUniqueName(), asset);
		}
		assetsByName.put(NULL_ENTITY_ASSET.getUniqueName(), NULL_ENTITY_ASSET);
		rebuild();
	}

	public void add(CreatureEntityAsset creatureEntityAsset) {
		assetsByName.put(creatureEntityAsset.getUniqueName(), creatureEntityAsset);
		rebuild();
	}

	public void rebuild() {
		this.typeMap = new CreatureEntityAssetsByType(entityAssetTypeDictionary, raceDictionary);
		for (CreatureEntityAsset asset : assetsByName.values()) {
			if (!NULL_ENTITY_ASSET.equals(asset)) {
				typeMap.add(asset);
			}
		}
	}

	public Map<String, CreatureEntityAsset> getAll() {
		return assetsByName;
	}

	public CreatureEntityAsset getMatching(EntityAssetType assetType, CreatureEntityAttributes attributes, Profession primaryProfession) {
		List<CreatureEntityAsset> allMatchingAssets = getAllMatchingAssets(assetType, attributes, primaryProfession);

		List<CreatureEntityAsset> matched = allMatchingAssets.stream().filter(creatureEntityAsset ->
				(creatureEntityAsset.getConsciousness() == null || creatureEntityAsset.getConsciousness().equals(attributes.getConsciousness())) &&
						(creatureEntityAsset.getSanity() == null || creatureEntityAsset.getSanity().equals(attributes.getSanity()))
		).collect(Collectors.toList());

		if (matched.size() > 0) {
			return matched.get(new RandomXS128(attributes.getSeed()).nextInt(matched.size()));
		} else {
			return null;
		}
	}

	public List<CreatureEntityAsset> getAllMatchingAssets(EntityAssetType assetType, CreatureEntityAttributes attributes, Profession primaryProfession) {
		return typeMap.getAll(assetType, attributes, primaryProfession);
	}

}
