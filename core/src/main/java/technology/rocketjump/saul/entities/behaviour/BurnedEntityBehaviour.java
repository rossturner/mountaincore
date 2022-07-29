package technology.rocketjump.saul.entities.behaviour;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.creature.StatusComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class BurnedEntityBehaviour implements BehaviourComponent {

	private Entity parentEntity;
	private double lastUpdateGameTime;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
		this.parentEntity = parentEntity;
	}

	@Override
	public BurnedEntityBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		BurnedEntityBehaviour cloned = new BurnedEntityBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime) {
		// Do nothing, does not update every frame
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		double gameTime = gameContext.getGameClock().getCurrentGameTime();
		double elapsed = gameTime - lastUpdateGameTime;
		lastUpdateGameTime = gameTime;

		StatusComponent statusComponent = parentEntity.getComponent(StatusComponent.class);
		if (statusComponent != null) {
			statusComponent.infrequentUpdate(elapsed);
		}
	}


	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// Nothing to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// Nothing to read
	}
}
