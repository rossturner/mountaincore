package technology.rocketjump.saul.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.ai.goap.actions.FaceTowardsLocationAction;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class StandToAttentionAction extends Action {

	private static final double MIN_HOURS_TO_IDLE = 0.1;
	private static final double MAX_HOURS_TO_IDLE = 0.3;

	public StandToAttentionAction(AssignedGoal parent) {
		super(parent);
	}

	private boolean initialised;
	private double elapsedTime;
	private double maxTime;

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!initialised) {
			FaceTowardsLocationAction faceTowardsLocationAction = null;

			if (gameContext.getRandom().nextBoolean()) {
				// 50/50 face a new direction
				CompassDirection randomDirection = CompassDirection.values()[gameContext.getRandom().nextInt(CompassDirection.values().length)];
				Vector2 facingTarget = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().cpy().add(randomDirection.toVector());
				faceTowardsLocationAction = new FaceTowardsLocationAction(parent); // push to head of queue
				parent.setTargetLocation(facingTarget);
			}
			if (faceTowardsLocationAction != null) {
				parent.actionQueue.push(faceTowardsLocationAction);
			}
			maxTime = gameContext.getGameClock().gameHoursToRealTimeSeconds(MIN_HOURS_TO_IDLE +
					(gameContext.getRandom().nextFloat() * (MAX_HOURS_TO_IDLE - MIN_HOURS_TO_IDLE)));

			initialised = true;
		}

		elapsedTime += deltaTime;
		if (elapsedTime > maxTime) {
			completionType = SUCCESS;
		}
	}



	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (initialised) {
			asJson.put("initialised", true);
		}
		if (elapsedTime > 0) {
			asJson.put("elapsedTime", elapsedTime);
		}
		if (maxTime > 0) {
			asJson.put("maxTime", maxTime);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.initialised = asJson.getBooleanValue("initialised");
		this.elapsedTime = asJson.getDoubleValue("elapsedTime");
		this.maxTime = asJson.getDoubleValue("maxTime");
	}
}
