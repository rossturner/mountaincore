package technology.rocketjump.saul.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.saul.entities.components.creature.ItemAssignmentComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.environment.GameClock;

public class GoalSelectionByItemAssignment implements GoalSelectionCondition {

    @JsonIgnore
    @Override
    public boolean apply(GameClock gameClock, Entity parentEntity) {
        ItemAssignmentComponent assignmentComponent = parentEntity.getComponent(ItemAssignmentComponent.class);
        if (assignmentComponent != null) {
            return !assignmentComponent.getHaulingAllocations().isEmpty();
        }
        return false;
    }
}
