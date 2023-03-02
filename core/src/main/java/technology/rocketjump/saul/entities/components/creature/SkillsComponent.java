package technology.rocketjump.saul.entities.components.creature;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Sets;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.jobs.model.SkillType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.saul.jobs.SkillDictionary.NULL_PROFESSION;
import static technology.rocketjump.saul.jobs.SkillDictionary.UNARMED_COMBAT_SKILL;

public class SkillsComponent implements EntityComponent {

	public static final int MAX_PROFESSIONS = 3;
	private List<Skill> activeProfessions = new ArrayList<>(); // Note this is in priority order
	private Map<Skill, Integer> skillLevels = new HashMap<>();
	private Map<Skill, Integer> experiencePoints = new HashMap<>();

	public SkillsComponent() {
		clear();
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		SkillsComponent cloned = new SkillsComponent();

		cloned.activeProfessions.addAll(this.activeProfessions);
		cloned.skillLevels.putAll(this.skillLevels);

		return cloned;
	}

	public void replace(Skill professionToReplace, Skill newProfession) {
		int indexToReplace = activeProfessions.indexOf(professionToReplace);
		if (indexToReplace == -1 && activeProfessions.size() < MAX_PROFESSIONS) {
			activeProfessions.add(newProfession);
		} else {
			activeProfessions.set(indexToReplace, newProfession);
		}
	}


	public void activateProfession(Skill profession) {
		if (!activeProfessions.contains(profession)) {
			// Insert new active profession before last entry (which is NULL_PROFESSION)
			activeProfessions.add(activeProfessions.size() - 1, profession);
		}
	}

	public void deactivateProfession(Skill profession) {
		if (profession.equals(NULL_PROFESSION)) {
			Logger.warn("Can not deactivate " + NULL_PROFESSION.getName());
		} else {
			activeProfessions.remove(profession);
		}
	}

	public void setSkillLevel(Skill skill, int skillLevel) {
		if (skill.getType().equals(SkillType.PROFESSION)) {
			activateProfession(skill);
		}
		skillLevels.put(skill, skillLevel);
	}

	public boolean hasActiveProfession(Skill profession) {
		return activeProfessions.contains(profession);
	}

	public boolean hasAnyActiveProfession(Set<Skill> professionSet) {
		return !Sets.intersection(professionSet, Sets.newHashSet(activeProfessions)).isEmpty();
	}

	public List<QuantifiedSkill> getActiveProfessions() {
		List<QuantifiedSkill> quantifiedSkills = new ArrayList<>();
		for (Skill profession : activeProfessions) {
			quantifiedSkills.add(new QuantifiedSkill(profession, skillLevels.getOrDefault(profession, 0)));
		}
		return quantifiedSkills;
	}

	public Skill getPrimaryProfession() {
		return activeProfessions.get(0);
	}

	public int getSkillLevel(Skill profession) {
		return skillLevels.getOrDefault(profession, 0);
	}

	public Set<Map.Entry<Skill, Integer>> getAll() {
		return skillLevels.entrySet();
	}

	public void experienceGained(int experiencePointsAmount, Skill profession) {
		int currentSkillLevel = getSkillLevel(profession);
		int currentExperience = experiencePoints.getOrDefault(profession, 0);
		currentExperience += experiencePointsAmount;

		while (currentExperience >= experienceRequiredForLevel(currentSkillLevel + 1) && currentSkillLevel < 100) {
			currentSkillLevel++;
			currentExperience -= experienceRequiredForLevel(currentSkillLevel);
		}

		skillLevels.put(profession, currentSkillLevel);
		experiencePoints.put(profession, currentExperience);
	}

	public float getNextLevelProgressPercent(Skill skill) {
		int skillLevel = getSkillLevel(skill);
		int nextLevelExp = experienceRequiredForLevel(skillLevel + 1);
		return (experiencePoints.getOrDefault(skill, 0) / (float) nextLevelExp);
	}

	private int experienceRequiredForLevel(int level) {
		return 4 + (level / 2);
	}

	public void clear() {
		activeProfessions.clear();
		skillLevels.clear();
		experiencePoints.clear();

		skillLevels.put(UNARMED_COMBAT_SKILL, 30);
		skillLevels.put(NULL_PROFESSION, 50); // always 50 for null/none profession so will take medium time
		activeProfessions.add(NULL_PROFESSION); // NULL_PROFESSION acts as default "Villager" profession
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONArray activeProfessionsJson = new JSONArray();
		for (Skill activeProfession : activeProfessions) {
			if (!activeProfession.equals(NULL_PROFESSION)) {
				activeProfessionsJson.add(activeProfession.getName());
			}
		}
		asJson.put("active", activeProfessionsJson);

		JSONObject skillLevelsJson = new JSONObject(true);
		for (Map.Entry<Skill, Integer> entry : skillLevels.entrySet()) {
			if (entry.getValue() > 0) {
				skillLevelsJson.put(entry.getKey().getName(), entry.getValue());
			}
		}
		asJson.put("skillLevels", skillLevelsJson);


		JSONObject experienceJson = new JSONObject(true);
		for (Map.Entry<Skill, Integer> entry : experiencePoints.entrySet()) {
			if (entry.getValue() > 0) {
				experienceJson.put(entry.getKey().getName(), entry.getValue());
			}
		}
		asJson.put("experience", experienceJson);
	}


	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray activeProfessionsJson = asJson.getJSONArray("active");
		JSONObject skillLevelsJson = asJson.getJSONObject("skillLevels");
		JSONObject experienceJson = asJson.getJSONObject("experience");
		if (activeProfessionsJson == null || skillLevelsJson == null || experienceJson == null) {
			throw new InvalidSaveException("Unrecognised professions json");
		}

		for (Object professionName: activeProfessionsJson) {
			Skill profession = relatedStores.skillDictionary.getByName(professionName.toString());
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			activeProfessions.add(activeProfessions.size() -1 , profession);
		}

		for (String professionName: skillLevelsJson.keySet()) {
			Skill profession = relatedStores.skillDictionary.getByName(professionName);
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			Integer skillLevel = skillLevelsJson.getInteger(professionName);
			if (skillLevel != null) {
				skillLevels.put(profession, skillLevel);
			}
		}


		for (String professionName: experienceJson.keySet()) {
			Skill profession = relatedStores.skillDictionary.getByName(professionName);
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			Integer points = experienceJson.getInteger(professionName);
			if (points != null) {
				experiencePoints.put(profession, points);
			}
		}
	}

	public void swapActiveProfessionPositions(int a, int b) {
		int lastIndex = activeProfessions.size() - 1;
		if (a > lastIndex || b > lastIndex) {
			return;
		}
		List<Skill> reordered = new ArrayList<>();
		for (int cursor = 0; cursor < activeProfessions.size(); cursor++) {
			if (cursor == a) {
				reordered.add(activeProfessions.get(b));
			} else  if (cursor == b) {
				reordered.add(activeProfessions.get(a));
			} else {
				reordered.add(activeProfessions.get(cursor));
			}
		}
		activeProfessions = reordered;
	}



	public static class QuantifiedSkill {

		private final Skill skill;
		private final int level;

		public QuantifiedSkill(Skill skill, int level) {
			this.skill = skill;
			this.level = level;
		}

		public Skill getSkill() {
			return skill;
		}

		public int getLevel() {
			return level;
		}

		@Override
		public String toString() {
			return "skill=" + skill + ", level=" + level;
		}
	}
}
