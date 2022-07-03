package technology.rocketjump.saul.entities.components.humanoid;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.common.collect.Sets;
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
	private Map<Profession, Float> skillLevels = new HashMap<>();

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

	public void add(Profession profession) {
		if (!activeProfessions.contains(profession)) {
			// Insert new active profession before last entry (which is NULL_PROFESSION)
			activeProfessions.add(activeProfessions.size() - 1, profession);
		}
		if (!skillLevels.containsKey(profession)) {
			skillLevels.put(profession, 0f);
		}
	}

	public void setSkillLevel(Profession profession, float skillLevel) {
		add(profession);
		skillLevels.put(profession, skillLevel);
	}

	public void deactivate(Profession profession) {
		if (profession.equals(NULL_PROFESSION)) {
			throw new IllegalArgumentException("Can not deactivate " + NULL_PROFESSION.getName());
		}
		activeProfessions.remove(profession);
	}

	public boolean hasActiveProfession(Profession profession) {
		return activeProfessions.contains(profession);
	}

	public boolean hasInactiveProfession(Profession profession) {
		return skillLevels.containsKey(profession) && !activeProfessions.contains(profession);
	}

	public boolean hasAnyActiveProfession(Set<Profession> professionSet) {
		return !Sets.intersection(professionSet, Set.of(activeProfessions)).isEmpty();
	}


	public List<QuantifiedProfession> getActiveProfessions() {
		List<QuantifiedProfession> quantifiedProfessions = new ArrayList<>();
		for (Profession profession : activeProfessions) {
			quantifiedProfessions.add(new QuantifiedProfession(profession, skillLevels.get(profession)));
		}
		return quantifiedProfessions;
	}

	public Profession getPrimaryProfession(Profession defaultProfession) {
		if (activeProfessions.get(0).equals(NULL_PROFESSION)) {
			return defaultProfession;
		} else {
			return activeProfessions.get(0);
		}
	}

	public float getSkillLevel(Profession profession) {
		if (profession.equals(NULL_PROFESSION)) {
			return 0.5f;
		} else {
			return skillLevels.getOrDefault(profession, 0f);
		}
	}

	public void clear() {
		activeProfessions.clear();
		skillLevels.put(NULL_PROFESSION, 0f);
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
		for (Map.Entry<Profession, Float> entry : skillLevels.entrySet()) {
			if (entry.getValue() > 0f) {
				skillLevelsJson.put(entry.getKey().getName(), entry.getValue());
			}
		}
		asJson.put("skillLevels", skillLevelsJson);
	}


	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray activeProfessionsJson = asJson.getJSONArray("active");
		JSONObject skillLevelsJson = asJson.getJSONObject("skillLevels");
		if (activeProfessionsJson == null || skillLevelsJson == null) {
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
			Float skillLevel = skillLevelsJson.getFloatValue(professionName);
			skillLevels.put(profession, skillLevel);
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
		private float skillLevel;

		public QuantifiedProfession(Profession profession, float skillLevel) {
			this.profession = profession;
			this.skillLevel = skillLevel;
		}

		public Profession getProfession() {
			return profession;
		}

		public float getSkillLevel() {
			return skillLevel;
		}

		@Override
		public String toString() {
			return "profession=" + profession + ", skillLevel=" + skillLevel;
		}
	}
}
