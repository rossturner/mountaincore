package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.components.creature.ItemAssignmentComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

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
        //No state here
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        //No state here
    }
}
