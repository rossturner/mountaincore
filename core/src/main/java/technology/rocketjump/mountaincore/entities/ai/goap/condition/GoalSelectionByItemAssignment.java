package technology.rocketjump.mountaincore.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.mountaincore.entities.components.creature.ItemAssignmentComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoalSelectionByItemAssignment implements GoalSelectionCondition {

    @JsonIgnore
    @Override
    public boolean apply(Entity parentEntity, GameContext gameContext) {
        ItemAssignmentComponent assignmentComponent = parentEntity.getComponent(ItemAssignmentComponent.class);
        if (assignmentComponent != null) {
            return !assignmentComponent.getHaulingAllocations().isEmpty();
        }
        return false;
    }
}
