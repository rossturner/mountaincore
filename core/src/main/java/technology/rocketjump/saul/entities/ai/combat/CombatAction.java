package technology.rocketjump.saul.entities.ai.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
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

	public abstract void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) throws ExitingCombatException;

	public abstract void interrupted(MessageDispatcher messageDispatcher); // do any cleanup when switching to other action mid-way through round

	public void onRoundCompletion() {
		if (completesInOneRound()) {
			this.completed = true;
		}
	}

	public abstract boolean completesInOneRound();

	public boolean isCompleted() {
		return completed;
	}
}
