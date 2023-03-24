package technology.rocketjump.mountaincore.entities.ai.goap;

import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;

public enum CreatureCategory {

	CIVILIAN,
	MILITARY,
	MERCHANT,
	INVADER;

	public static CreatureCategory getCategoryFor(Entity entity) {
		FactionComponent factionComponent = entity.getOrCreateComponent(FactionComponent.class);
		if (factionComponent.getFaction().equals(Faction.MERCHANTS)) {
			return MERCHANT;
		} else if (factionComponent.getFaction().equals(Faction.HOSTILE_INVASION)) {
			return INVADER;
		} else {
			MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
			return militaryComponent != null && militaryComponent.isInMilitary() ? MILITARY : CIVILIAN;
		}
	}

}
