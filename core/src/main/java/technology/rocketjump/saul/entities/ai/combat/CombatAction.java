package technology.rocketjump.saul.entities.ai.combat;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.persistence.model.ChildPersistable;

/**
 * Class to represent the action a creature in combat is taking in a single round or across multiple rounds
 */
public abstract class CombatAction implements ChildPersistable {

	protected Entity parentEntity;
	protected boolean completed;

	public CombatAction(Entity parentEntity) {
		this.parentEntity = parentEntity;
	}

	public void onRoundCompletion() {
		if (completesInOneRound()) {
			this.completed = true;
		}
	}

	public abstract boolean completesInOneRound();

}
