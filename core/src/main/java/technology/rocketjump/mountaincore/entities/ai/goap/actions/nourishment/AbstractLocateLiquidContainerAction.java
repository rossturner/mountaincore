package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

public abstract class AbstractLocateLiquidContainerAction extends Action implements RequestHaulingAllocationMessage.ItemAllocationCallback {
    public AbstractLocateLiquidContainerAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    public void update(float deltaTime, GameContext gameContext) {

        requestHaulingAllocation();

        if (completionType == null) {
            completionType = CompletionType.FAILURE;
        }
    }

    protected abstract void requestHaulingAllocation();

    @Override
    public void allocationFound(HaulingAllocation haulingAllocation) {
        if (haulingAllocation != null) {
            parent.setAssignedHaulingAllocation(haulingAllocation);
            completionType = CompletionType.SUCCESS;
        }
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
    }
}
