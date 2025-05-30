package technology.rocketjump.mountaincore.assets.entities.creature;

import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.jobs.model.Skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static technology.rocketjump.mountaincore.jobs.SkillDictionary.NULL_PROFESSION;

public class CreatureEntityAssetsByProfession {

	private Map<String, List<CreatureEntityAsset>> professionNameMap = new HashMap<>();

	public static final CreatureEntityAsset NULL_ENTITY_ASSET;

	static {
		NULL_ENTITY_ASSET = new CreatureEntityAsset();
		NULL_ENTITY_ASSET.setBodyShape(CreatureBodyShape.ANY);
		NULL_ENTITY_ASSET.setGender(Gender.ANY);
		NULL_ENTITY_ASSET.setRace(null);
		NULL_ENTITY_ASSET.setType(null);
		NULL_ENTITY_ASSET.setUniqueName("None");
	}

	public CreatureEntityAssetsByProfession() {
		professionNameMap.put(NULL_PROFESSION.getName(), new ArrayList<>());
	}

	public void add(CreatureEntityAsset asset) {
		String professionName = asset.getProfession();
		if (professionName == null) {
			professionName = NULL_PROFESSION.getName();
		}
		professionNameMap.computeIfAbsent(professionName, a -> new ArrayList<>()).add(asset);
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Skill primaryProfession) {
		List<CreatureEntityAsset> assetsForProfession = professionNameMap.get(primaryProfession.getName());

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			assetsForProfession = professionNameMap.get(NULL_PROFESSION.getName());
		}

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			return NULL_ENTITY_ASSET;
		} else {
			return assetsForProfession.get((Math.abs((int)attributes.getSeed())) % assetsForProfession.size());
		}
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Skill primaryProfession) {
		final Skill profession = primaryProfession == null ? NULL_PROFESSION : primaryProfession;
		List<CreatureEntityAsset> assetsForProfession = professionNameMap.get(profession.getName());

		if (assetsForProfession == null || assetsForProfession.isEmpty()) {
			assetsForProfession = professionNameMap.get(NULL_PROFESSION.getName());
		}

		return assetsForProfession.stream().filter(a -> a.matches(attributes, profession)).collect(Collectors.toList());
	}
}
