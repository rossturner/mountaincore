package technology.rocketjump.mountaincore.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.jobs.model.SkillType;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class SkillDictionary {

	private Map<String, Skill> byName = new HashMap<>();
	private Map<SkillType, List<Skill>> byType = new HashMap<>();
	private List<Skill> selectableProfessions = new ArrayList<>();

	public static Skill NULL_PROFESSION = new Skill();
	public static Skill UNARMED_COMBAT_SKILL = new Skill();
	public static Skill CONTEXT_DEPENDENT_PROFESSION_REQUIRED = new Skill();
	static {
		NULL_PROFESSION.setName("NULL_PROFESSION");
		NULL_PROFESSION.setType(SkillType.PROFESSION);
		NULL_PROFESSION.setI18nKey("PROFESSION.HAULER");
		NULL_PROFESSION.setIcon("settlers_job_hauler");
		NULL_PROFESSION.setDraggableIcon("btn_drag_job_hauler");
		CONTEXT_DEPENDENT_PROFESSION_REQUIRED.setName("Specific profession required");

		UNARMED_COMBAT_SKILL.setName("UNARMED_COMBAT");
		UNARMED_COMBAT_SKILL.setType(SkillType.COMBAT_SKILL);
		UNARMED_COMBAT_SKILL.setI18nKey("SKILL.UNARMED_COMBAT");
	}

	@Inject
	public SkillDictionary() throws IOException {
		this(new File("assets/definitions/types/skills.json"));
	}

	public SkillDictionary(File professionsJsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Skill> skills = objectMapper.readValue(FileUtils.readFileToString(professionsJsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, Skill.class));

		byName.put(NULL_PROFESSION.getName(), NULL_PROFESSION);
		byName.put(UNARMED_COMBAT_SKILL.getName(), UNARMED_COMBAT_SKILL);
		for (Skill skill : skills) {
			byName.put(skill.getName(), skill);
			byType.computeIfAbsent(skill.getType(), a -> new ArrayList<>()).add(skill);
			if (SkillType.PROFESSION.equals(skill.getType()) && skill.isSelectableByPlayer()) {
				selectableProfessions.add(skill);
			}
		}
	}

	public Skill getByName(Object name) {
		return byName.get(name);
	}
	public List<Skill> getAllProfessions() {
		return byType.get(SkillType.PROFESSION);
	}

	public List<Skill> getSelectableProfessions() {
		return selectableProfessions;
	}

	public List<Skill> getAllCombatSkills() {
		return byType.get(SkillType.COMBAT_SKILL);
	}

	public Collection<Skill> getAll() {
		return byName.values();
	}
}
