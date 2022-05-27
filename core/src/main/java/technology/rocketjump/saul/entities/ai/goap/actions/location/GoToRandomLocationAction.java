package technology.rocketjump.saul.entities.ai.goap.actions.location;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.entities.ai.goap.actions.IdleAction.pickRandomLocation;

public class GoToRandomLocationAction extends GoToLocationAction {

	private static final float MAX_ELAPSED_TIME = 10f;

	private float elapsedTime;

	public GoToRandomLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (overrideLocation == null) {
			Vector2 target = pickRandomLocation(gameContext, parent.parentEntity);
			if (target != null) {
				overrideLocation = target;
			} else {
				completionType = FAILURE;
				return;
			}
		}

		elapsedTime += deltaTime;
		if (elapsedTime > MAX_ELAPSED_TIME) {
			completionType = SUCCESS;
		}

		super.update(deltaTime, gameContext);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("elapsed", elapsedTime);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.elapsedTime = asJson.getFloatValue("elapsed");
	}
}
