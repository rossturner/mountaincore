package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoToHaulingTargetLocationAction extends GoToLocationAction {
    public GoToHaulingTargetLocationAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    protected Vector2 selectDestination(GameContext gameContext) {
        return calculatePosition(parent.getAssignedHaulingAllocation().getTargetPositionType(),
                parent.getAssignedHaulingAllocation().getTargetPosition(),
                parent.getAssignedHaulingAllocation().getTargetId(), gameContext); // FIXME this should be targetContainerId rather than targetId, but its not yet implemented yet
    }
}
