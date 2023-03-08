package technology.rocketjump.saul.entities.model.physical.creature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviourDictionary;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyStructureDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.SpeciesColor;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.String.format;
import static technology.rocketjump.saul.entities.ai.goap.ScheduleDictionary.NULL_SCHEDULE;
import static technology.rocketjump.saul.entities.model.EntityType.CREATURE;
import static technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary.initialiseSpeciesColor;

@Singleton
public class RaceDictionary {

	private final GameMaterialDictionary gameMaterialDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final BodyStructureDictionary bodyStructureDictionary;
	private final CreatureBehaviourDictionary creatureBehaviourDictionary;
	private final ScheduleDictionary scheduleDictionary;
	private final SkillDictionary skillDictionary;
	private final SoundAssetDictionary soundAssetDictionary;
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private final Map<String, Race> byName = new HashMap<>();

	@Inject
	public RaceDictionary(GameMaterialDictionary gameMaterialDictionary, ItemTypeDictionary itemTypeDictionary, BodyStructureDictionary bodyStructureDictionary,
						  CreatureBehaviourDictionary creatureBehaviourDictionary, ScheduleDictionary scheduleDictionary,
						  SkillDictionary skillDictionary, SoundAssetDictionary soundAssetDictionary,
						  ParticleEffectTypeDictionary particleEffectTypeDictionary) throws IOException {
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.bodyStructureDictionary = bodyStructureDictionary;
		this.creatureBehaviourDictionary = creatureBehaviourDictionary;
		this.scheduleDictionary = scheduleDictionary;
		this.skillDictionary = skillDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;

		ObjectMapper objectMapper = new ObjectMapper();
		File itemTypeJsonFile = new File("assets/definitions/types/races.json");
		List<Race> raceList = objectMapper.readValue(FileUtils.readFileToString(itemTypeJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Race.class));

		for (Race race : raceList) {
			initialise(race);
			byName.put(race.getName(), race);
		}

		for (ItemType itemType : itemTypeDictionary.getAll()) {
			if (itemType.getDefenseInfo() != null) {
				if (itemType.getDefenseInfo().getRestrictedToRaceNames() != null) {
					for (String raceName : itemType.getDefenseInfo().getRestrictedToRaceNames()) {
						Race race = getByName(raceName);
						if (race == null) {
							Logger.error("Could not find race " + raceName + " for defense info on item type " + itemType.getItemTypeName());
						} else {
							itemType.getDefenseInfo().getRestrictedToRaces().add(race);
						}
					}
				}
			}
		}

	}

	public Race getByName(String name) {
		return byName.get(name);
	}

	public void add(Race newRace) {
		String name = newRace.getName();
		if (byName.containsKey(name)) {
			throw new RuntimeException("Cannot add duplicate race: " + name);
		}
		initialise(newRace);
		byName.put(name, newRace);
	}

	public Collection<Race> getAll() {
		return byName.values();
	}

	private void initialise(Race race) {
		race.setBodyStructure(bodyStructureDictionary.getByName(race.getBodyStructureName()));
		if (race.getBodyStructure() == null) {
			throw new RuntimeException(format("Could not find body structure with name %s for race %s", race.getBodyStructureName(), race.getName()));
		}

		WeaponInfo unarmedWeapon = race.getFeatures().getUnarmedWeapon();
		if (unarmedWeapon != null) {
			unarmedWeapon.initialise(race.getName(), soundAssetDictionary, particleEffectTypeDictionary, skillDictionary);
		}

		if (race.getFeatures().getMeat() != null) {
			race.getFeatures().getMeat().setItemType(itemTypeDictionary.getByName(race.getFeatures().getMeat().getItemTypeName()));
			if (race.getFeatures().getMeat().getItemType() == null) {
				Logger.error("Could not find item type " + race.getFeatures().getMeat().getItemTypeName() +
						" for meat as part of race " + race.getName());
			}
			race.getFeatures().getMeat().setMaterial(gameMaterialDictionary.getByName(race.getFeatures().getMeat().getMaterialName()));
			if (race.getFeatures().getMeat().getMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getMeat().getMaterialName() +
						" for meat as part of race " + race.getName());
			}
		}

		if (race.getFeatures().getBones() != null) {
			race.getFeatures().getBones().setMaterial(gameMaterialDictionary.getByName(race.getFeatures().getBones().getMaterialName()));
			if (race.getFeatures().getBones().getMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getBones().getMaterialName() +
						" for bones as part of race " + race.getName());
			}
		}

		for (SpeciesColor speciesColor : race.getColors().values()) {
			initialiseSpeciesColor(CREATURE, speciesColor);
		}

		if (race.getFeatures().getSkin() != null && race.getFeatures().getSkin().getMaterialName() != null) {
			race.getFeatures().getSkin().setItemType(itemTypeDictionary.getByName(race.getFeatures().getSkin().getItemTypeName()));
			if (race.getFeatures().getSkin().getItemType() == null) {
				Logger.error("Could not find item type " + race.getFeatures().getSkin().getItemTypeName() +
						" for meat as part of race " + race.getName());
			}

			race.getFeatures().getSkin().setMaterial(gameMaterialDictionary.getByName(race.getFeatures().getSkin().getMaterialName()));
			if (race.getFeatures().getSkin().getMaterial() == null) {
				Logger.error("Could not find material " + race.getFeatures().getSkin().getMaterialName() +
						" for skin as part of race " + race.getName());
			}
		}

		if (race.getBehaviour().getBehaviourName() != null) {
			race.getBehaviour().setBehaviourClass(creatureBehaviourDictionary.getByName(race.getBehaviour().getBehaviourName()));
			if (race.getBehaviour().getBehaviourClass() == null) {
				Logger.error(String.format("Could not find behaviour with name %s for race %s", race.getBehaviour().getBehaviourName(), race.getName()));
			}
		}
		if (race.getBehaviour().getScheduleName() != null) {
			race.getBehaviour().setSchedule(scheduleDictionary.getByName(race.getBehaviour().getScheduleName()));
			if (race.getBehaviour().getSchedule() == null) {
				Logger.error(String.format("Could not find scheudle %s for race %s", race.getBehaviour().getBehaviourName(), race.getName()));
				race.getBehaviour().setSchedule(NULL_SCHEDULE);
			}
		}

	}
}
