package technology.rocketjump.saul.assets.entities.creature;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.jobs.model.Skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetsByProfession.NULL_ENTITY_ASSET;

public class CreatureEntityAssetsByRace {

	private Map<Race, CreatureEntityAssetsByBodyType> raceMap = new HashMap<>();

	public CreatureEntityAssetsByRace() {
	}

	public void add(CreatureEntityAsset asset) {
		Race race = asset.getRace();
		if (race == null) {
			Logger.error("Race must be specified on CreatureEntityAsset " + asset.getUniqueName());
			return;
		}
		raceMap.computeIfAbsent(race, a -> new CreatureEntityAssetsByBodyType()).add(asset);
	}

	public CreatureEntityAsset get(CreatureEntityAttributes attributes, Skill primaryProfession) {
		Race race = attributes.getRace();
		CreatureEntityAssetsByBodyType childMap = raceMap.get(race);
		return childMap != null ? childMap.get(attributes, primaryProfession) : NULL_ENTITY_ASSET;
	}

	public List<CreatureEntityAsset> getAll(CreatureEntityAttributes attributes, Skill primaryProfession) {
		Race race = attributes.getRace();
		CreatureEntityAssetsByBodyType childMap = raceMap.get(race);
		return childMap != null ? childMap.getAll(attributes, primaryProfession) : List.of();
	}
}
