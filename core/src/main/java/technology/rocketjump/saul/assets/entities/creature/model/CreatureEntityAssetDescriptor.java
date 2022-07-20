package technology.rocketjump.saul.assets.entities.creature.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.misc.Name;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private EntityAssetType type;
	private String raceName;
	@JsonIgnore
	private Race race;
	private CreatureBodyShape bodyShape;
	private Gender gender;
	private String profession;
	private Consciousness consciousness;
	private List<Consciousness> consciousnessList;
	private Sanity sanity;

	private Map<String, List<String>> tags = new HashMap<>();

	public boolean matches(CreatureEntityAttributes entityAttributes, Profession primaryProfession) {
		if (race != null && !race.equals(entityAttributes.getRace())) {
			return false;
		}
		if (bodyShape != null && !bodyShape.equals(CreatureBodyShape.ANY) && !bodyShape.equals(entityAttributes.getBodyShape())) {
			return false;
		}
		if (gender != null && !gender.equals(Gender.ANY) && !gender.equals(entityAttributes.getGender())) {
			return false;
		}
		if (profession != null && primaryProfession != null && !profession.equals(primaryProfession.getName())) {
			return false;
		}
		if (consciousness != null && entityAttributes.getConsciousness() != consciousness) {
			return false;
		}
		if (consciousnessList != null && !consciousnessList.isEmpty() && !consciousnessList.contains(entityAttributes.getConsciousness())) {
			return false;
		}
		if (sanity != null && entityAttributes.getSanity() != sanity) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
		if (race != null) {
			this.raceName = race.getName();
		}
	}

	public CreatureBodyShape getBodyShape() {
		return bodyShape;
	}

	public void setBodyShape(CreatureBodyShape bodyShape) {
		this.bodyShape = bodyShape;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getProfession() {
		return profession;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public Consciousness getConsciousness() {
		return consciousness;
	}

	public void setConsciousness(Consciousness consciousness) {
		this.consciousness = consciousness;
	}

	public Sanity getSanity() {
		return sanity;
	}

	public void setSanity(Sanity sanity) {
		this.sanity = sanity;
	}

	public String getRaceName() {
		return raceName;
	}

	public void setRaceName(String raceName) {
		this.raceName = raceName;
	}

	public List<Consciousness> getConsciousnessList() {
		return consciousnessList;
	}

	public void setConsciousnessList(List<Consciousness> consciousnessList) {
		this.consciousnessList = consciousnessList;
	}

	@Override
	public String toString() {
		return getUniqueName();
	}
}
