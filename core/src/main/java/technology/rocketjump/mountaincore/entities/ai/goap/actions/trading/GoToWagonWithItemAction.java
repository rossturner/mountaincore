package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoToWagonWithItemAction extends GoToLocationAction {
	public GoToWagonWithItemAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getPlannedTrade() != null) {
			Entity sourceEntity = gameContext.getEntity(parent.getPlannedTrade().getHaulingAllocation().getSourceContainerId());
			Entity targetEntity = gameContext.getEntity(parent.getPlannedTrade().getHaulingAllocation().getTargetId());

			if (sourceEntity != null && sourceEntity.getType().equals(EntityType.VEHICLE)) {
				return sourceEntity.getLocationComponent().getWorldOrParentPosition();
			} else if (targetEntity != null && targetEntity.getType().equals(EntityType.VEHICLE)) {
				return targetEntity.getLocationComponent().getWorldOrParentPosition();
			}
		}
		return null;
	}
}
