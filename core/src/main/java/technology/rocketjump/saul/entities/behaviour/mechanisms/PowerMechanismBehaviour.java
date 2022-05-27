package technology.rocketjump.saul.entities.behaviour.mechanisms;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.underground.PowerGrid;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class PowerMechanismBehaviour implements BehaviourComponent {

	private Entity parentEntity;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		return null;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		PowerGrid powerGrid = parentTile.getUnderTile().getPowerGrid();

		if (powerGrid != null && powerGrid.getTotalPowerAvailable() > 0) {
			float animationProgress = parentEntity.getPhysicalEntityComponent().getAnimationProgress();
			float animationSpeed = 1f;
			animationProgress += deltaTime * animationSpeed;
			while (animationProgress > 1f) {
				animationProgress -= 1f;
			}
			parentEntity.getPhysicalEntityComponent().setAnimationProgress(animationProgress);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {

	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return false;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
