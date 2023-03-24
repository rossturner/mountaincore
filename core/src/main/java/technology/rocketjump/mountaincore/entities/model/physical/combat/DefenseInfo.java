package technology.rocketjump.mountaincore.entities.model.physical.combat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefenseInfo {

	public static final DefenseInfo NONE = new DefenseInfo();
	static {
		NONE.maxDefensePoints = 0;
		NONE.maxDefenseRegainedPerRound = 0;
	}

	private DefenseType type = DefenseType.RACIAL;
	private Integer maxDefensePoints;
	private Integer maxDefenseRegainedPerRound;
	private Map<CombatDamageType, Integer> damageReduction = new EnumMap<>(CombatDamageType.class);

	// The restriction by race is only intended to apply to the ARMOR DefenseType
	private List<String> restrictedToRaceNames = new ArrayList<>();
	@JsonIgnore
	private Set<Race> restrictedToRaces = new HashSet<>();

	public boolean canBeEquippedBy(Entity entity) {
		if (restrictedToRaces == null || restrictedToRaces.isEmpty()) {
			return true;
		} else {
			if (entity != null && entity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes) {
				return restrictedToRaces.contains(creatureEntityAttributes.getRace());
			} else {
				return false;
			}
		}
	}

	public DefenseType getType() {
		return type;
	}

	public void setType(DefenseType type) {
		this.type = type;
	}

	public Integer getMaxDefensePoints() {
		return maxDefensePoints;
	}

	public void setMaxDefensePoints(Integer maxDefensePoints) {
		this.maxDefensePoints = maxDefensePoints;
	}

	public Integer getMaxDefenseRegainedPerRound() {
		return maxDefenseRegainedPerRound;
	}

	public void setMaxDefenseRegainedPerRound(Integer maxDefenseRegainedPerRound) {
		this.maxDefenseRegainedPerRound = maxDefenseRegainedPerRound;
	}

	public Map<CombatDamageType, Integer> getDamageReduction() {
		return damageReduction;
	}

	public void setDamageReduction(Map<CombatDamageType, Integer> damageReduction) {
		this.damageReduction = damageReduction;
	}

	public List<String> getRestrictedToRaceNames() {
		return restrictedToRaceNames;
	}

	public void setRestrictedToRaceNames(List<String> restrictedToRaceNames) {
		this.restrictedToRaceNames = restrictedToRaceNames;
	}

	public Set<Race> getRestrictedToRaces() {
		return restrictedToRaces;
	}

	public void setRestrictedToRaces(Set<Race> restrictedToRaces) {
		this.restrictedToRaces = restrictedToRaces;
	}
}
