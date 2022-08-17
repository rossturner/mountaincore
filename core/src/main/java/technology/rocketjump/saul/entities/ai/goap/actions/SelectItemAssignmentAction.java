package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.components.creature.ItemAssignmentComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import java.util.List;

public class SelectItemAssignmentAction extends Action {
    public SelectItemAssignmentAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
        ItemAssignmentComponent itemAssignmentComponent = parent.parentEntity.getOrCreateComponent(ItemAssignmentComponent.class);
        List<HaulingAllocation> haulingAllocations = itemAssignmentComponent.getHaulingAllocations();
        if (!haulingAllocations.isEmpty()) {
            HaulingAllocation haulingAllocation = haulingAllocations.remove(0);
            parent.setAssignedHaulingAllocation(haulingAllocation);
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
