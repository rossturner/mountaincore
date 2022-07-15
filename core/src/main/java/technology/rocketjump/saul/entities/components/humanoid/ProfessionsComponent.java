package technology.rocketjump.saul.entities.components.humanoid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Sets;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.saul.jobs.ProfessionDictionary.NULL_PROFESSION;

public class ProfessionsComponent implements EntityComponent {

	public static final int MAX_PROFESSIONS = 3;
	private List<Profession> activeProfessions = new ArrayList<>(); // Note this is in priority order
	private Map<Profession, Integer> skillLevels = new HashMap<>();
	private Map<Profession, Integer> experiencePoints = new HashMap<>();

	public ProfessionsComponent() {
		clear();
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ProfessionsComponent cloned = new ProfessionsComponent();

		cloned.activeProfessions.addAll(this.activeProfessions);
		cloned.skillLevels.putAll(this.skillLevels);

		return cloned;
	}

	public void activate(Profession profession) {
		if (!activeProfessions.contains(profession)) {
			// Insert new active profession before last entry (which is NULL_PROFESSION)
			activeProfessions.add(activeProfessions.size() - 1, profession);
		}
	}

	public void deactivate(Profession profession) {
		if (profession.equals(NULL_PROFESSION)) {
			Logger.warn("Can not deactivate " + NULL_PROFESSION.getName());
		} else {
			activeProfessions.remove(profession);
		}
	}

	public void setSkillLevel(Profession profession, int skillLevel) {
		activate(profession);
		skillLevels.put(profession, skillLevel);
	}

	public boolean hasActiveProfession(Profession profession) {
		return activeProfessions.contains(profession);
	}

	public boolean hasAnyActiveProfession(Set<Profession> professionSet) {
		return !Sets.intersection(professionSet, Sets.newHashSet(activeProfessions)).isEmpty();
	}

	public List<QuantifiedProfession> getActiveProfessions() {
		List<QuantifiedProfession> quantifiedProfessions = new ArrayList<>();
		for (Profession profession : activeProfessions) {
			quantifiedProfessions.add(new QuantifiedProfession(profession, skillLevels.getOrDefault(profession, 0)));
		}
		return quantifiedProfessions;
	}

	public Profession getPrimaryProfession() {
		return activeProfessions.get(0);
	}

	public int getSkillLevel(Profession profession) {
		return skillLevels.getOrDefault(profession, 0);
	}

	public void experienceGained(int experiencePointsAmount, Profession profession) {
		int currentSkillLevel = getSkillLevel(profession);
		int currentExperience = experiencePoints.getOrDefault(profession, 0);
		currentExperience += experiencePointsAmount;

		while (currentExperience >= currentSkillLevel + 1 && currentSkillLevel < 100) {
			currentSkillLevel++;
			currentExperience -= currentSkillLevel;
		}

		skillLevels.put(profession, currentSkillLevel);
		experiencePoints.put(profession, currentExperience);
	}

	public void clear() {
		activeProfessions.clear();
		skillLevels.clear();
		experiencePoints.clear();

		skillLevels.put(NULL_PROFESSION, 50); // always 50 for null/none profession so will take medium time
		activeProfessions.add(NULL_PROFESSION); // NULL_PROFESSION acts as default "Villager" profession
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONArray activeProfessionsJson = new JSONArray();
		for (Profession activeProfession : activeProfessions) {
			if (!activeProfession.equals(NULL_PROFESSION)) {
				activeProfessionsJson.add(activeProfession.getName());
			}
		}
		asJson.put("active", activeProfessionsJson);

		JSONObject skillLevelsJson = new JSONObject(true);
		for (Map.Entry<Profession, Integer> entry : skillLevels.entrySet()) {
			if (entry.getValue() > 0) {
				skillLevelsJson.put(entry.getKey().getName(), entry.getValue());
			}
		}
		asJson.put("skillLevels", skillLevelsJson);


		JSONObject experienceJson = new JSONObject(true);
		for (Map.Entry<Profession, Integer> entry : experiencePoints.entrySet()) {
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
			Profession profession = relatedStores.professionDictionary.getByName(professionName.toString());
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			activeProfessions.add(activeProfessions.size() -1 , profession);
		}

		for (String professionName: skillLevelsJson.keySet()) {
			Profession profession = relatedStores.professionDictionary.getByName(professionName);
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			Integer skillLevel = skillLevelsJson.getInteger(professionName);
			if (skillLevel != null) {
				skillLevels.put(profession, skillLevel);
			}
		}


		for (String professionName: experienceJson.keySet()) {
			Profession profession = relatedStores.professionDictionary.getByName(professionName);
			if (profession == null) {
				throw new InvalidSaveException("Could not find profession with name " + professionName);
			}
			Integer points = experienceJson.getInteger(professionName);
			if (points != null) {
				experiencePoints.put(profession, points);
			}
		}
	}

	public void swapActivePositions(int a, int b) {
		List<Profession> reordered = new ArrayList<>();
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


	public static class QuantifiedProfession {

		private final Profession profession;
		private final int skillLevel;

		public QuantifiedProfession(Profession profession, int skillLevel) {
			this.profession = profession;
			this.skillLevel = skillLevel;
		}

		public Profession getProfession() {
			return profession;
		}

		public int getSkillLevel() {
			return skillLevel;
		}

		@Override
		public String toString() {
			return "profession=" + profession + ", skillLevel=" + skillLevel;
		}
	}
}
