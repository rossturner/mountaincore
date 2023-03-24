package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class CancelFoodAllocationAction extends Action {
	public CancelFoodAllocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getFoodAllocation() != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.FOOD_ALLOCATION_CANCELLED, parent.getFoodAllocation());
			parent.setFoodAllocation(null);
			completionType = CompletionType.SUCCESS;
		} else {
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
