package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoToHaulingSourceLocationAction extends GoToLocationAction {
    public GoToHaulingSourceLocationAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    protected Vector2 selectDestination(GameContext gameContext) {
        if (parent.getAssignedHaulingAllocation() != null && parent.getAssignedHaulingAllocation().getSourcePosition() != null) {
            if (parent.getAssignedHaulingAllocation().getHauledEntityType().equals(EntityType.FURNITURE)) {
                return calculatePosition(parent.getAssignedHaulingAllocation().getSourcePositionType(),
                        parent.getAssignedHaulingAllocation().getSourcePosition(),
                        parent.getAssignedHaulingAllocation().getHauledEntityId(), gameContext);
            } else {
                return calculatePosition(parent.getAssignedHaulingAllocation().getSourcePositionType(),
                        parent.getAssignedHaulingAllocation().getSourcePosition(),
                        parent.getAssignedHaulingAllocation().getSourceContainerId(), gameContext);
            }
        }
        return null;
    }
}
