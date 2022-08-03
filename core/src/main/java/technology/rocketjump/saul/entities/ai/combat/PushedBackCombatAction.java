package technology.rocketjump.saul.entities.ai.combat;

import technology.rocketjump.saul.entities.model.Entity;

// Extends DefensiveCombatAction to recover defense pool if it was already defending
public class PushedBackCombatAction extends DefensiveCombatAction {
	public PushedBackCombatAction(Entity parentEntity) {
		super(parentEntity);
	}
}
