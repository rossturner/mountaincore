package technology.rocketjump.saul.entities.model.physical.combat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.EnumMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefenseInfo {

	public static final DefenseInfo NONE = new DefenseInfo();
	static {
		NONE.maxDefensePoints = 0;
		NONE.maxDefenseRegainedPerRound = 0;
	}

	private Integer maxDefensePoints;
	private Integer maxDefenseRegainedPerRound;
	private Map<CombatDamageType, Integer> damageReduction = new EnumMap<>(CombatDamageType.class);

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

}
