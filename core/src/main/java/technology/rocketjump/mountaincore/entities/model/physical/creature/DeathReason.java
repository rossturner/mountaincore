package technology.rocketjump.mountaincore.entities.model.physical.creature;

public enum DeathReason {

	STARVATION,
	DEHYDRATION,
	BURNING,
	FOOD_POISONING,
	EXHAUSTION,
	CRUSHED_BY_FALLING_DEBRIS,
	FROZEN,
	INTERNAL_BLEEDING,
	CRITICAL_ORGAN_DAMAGE,
	SUFFOCATION,
	GIVEN_UP_ON_LIFE,
	EXTENSIVE_INJURIES,
	KILLED_BY_ENTITY,
	UNKNOWN;

	public String getI18nKey() {
		return "DEATH_REASON."+name();
	}


}
