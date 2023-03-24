package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class FaceTowardsLocationAction extends Action {

	public FaceTowardsLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		return selectTargetLocation(gameContext) != null;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		Vector2 target = selectTargetLocation(gameContext);
		if (target == null) {
			completionType = FAILURE;
		} else {
			Vector2 vectorToTarget = target.sub(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
			parent.parentEntity.getOwnOrVehicleLocationComponent().setFacing(vectorToTarget);
			completionType = SUCCESS;
		}
	}

	private Vector2 selectTargetLocation(GameContext gameContext) {
		if (parent.getAssignedFurnitureId() != null) {
			Entity entity = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			if (entity != null) {
				return entity.getLocationComponent().getWorldPosition().cpy();
			}
			Logger.error("Entity ID assigned does not exist in gameContext");
		}
		if (parent.getAssignedJob() != null) {
			if (parent.getAssignedJob().getType().isAccessedFromAdjacentTile()) {
				return VectorUtils.toVector(parent.getAssignedJob().getJobLocation());
			} else if (parent.getAssignedJob().getSecondaryLocation() != null) {
				return VectorUtils.toVector(parent.getAssignedJob().getSecondaryLocation());
			} else {
				return VectorUtils.toVector(parent.getAssignedJob().getJobLocation());
			}
		} else {
			return parent.getTargetLocation();
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

}
