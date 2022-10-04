package technology.rocketjump.saul.entities.ai.goap;

import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;

public enum CreatureCategory {

	CIVILIAN,
	MILITARY,
	INVADER;

	public static CreatureCategory getCategoryFor(Entity entity) {
		FactionComponent factionComponent = entity.getOrCreateComponent(FactionComponent.class);
		if (factionComponent.getFaction().equals(Faction.HOSTILE_INVASION)) {
			return INVADER;
		} else {
			MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
			return militaryComponent != null && militaryComponent.isInMilitary() ? MILITARY : CIVILIAN;
		}
	}

}
