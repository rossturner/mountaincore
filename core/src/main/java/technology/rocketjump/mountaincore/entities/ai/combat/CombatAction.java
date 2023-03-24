package technology.rocketjump.mountaincore.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

/**
 * Class to represent the action a creature in combat is taking in a single round or across multiple rounds
 */
public abstract class CombatAction implements ChildPersistable {

	protected Entity parentEntity;
	protected boolean completed;

	public CombatAction(Entity parentEntity) {
		this.parentEntity = parentEntity;
	}

	public static CombatAction newInstance(Class<?> classType, Entity parentEntity) {
		try {
			return (CombatAction) classType.getConstructor(parentEntity.getClass()).newInstance(parentEntity);
		} catch (ReflectiveOperationException e) {
			Logger.error("Could not find constructor for class " + classType.getSimpleName() + " with a parameter of " + parentEntity.getClass().getSimpleName());
			throw new RuntimeException(e);
		}
	}

	public abstract void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) throws ExitingCombatException;

	public abstract void interrupted(MessageDispatcher messageDispatcher); // do any cleanup when switching to other action mid-way through round

	public void onRoundCompletion() {
		if (completesInOneRound()) {
			this.completed = true;
		}
	}

	// For initialisation after loading from disk
	public void setParentEntity(Entity parentEntity) {
		this.parentEntity = parentEntity;
	}

	public abstract boolean completesInOneRound();

	public boolean isCompleted() {
		return completed;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (completed) {
			asJson.put("completed", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.completed = asJson.getBooleanValue("completed");
	}
}
