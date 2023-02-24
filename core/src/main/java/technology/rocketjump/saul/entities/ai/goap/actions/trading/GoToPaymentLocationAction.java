package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.gamecontext.GameContext;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class GoToPaymentLocationAction  extends GoToLocationAction {

	public GoToPaymentLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getPlannedTrade() != null) {
			Entity itemEntity = gameContext.getEntity(parent.getPlannedTrade().getPaymentItemAllocation().getTargetItemEntityId());
			if (itemEntity != null) {
				Entity containerEntity = itemEntity.getLocationComponent().getContainerEntity();
				if (containerEntity != null && containerEntity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
					FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getNearestNavigableWorkspace(containerEntity, gameContext.getAreaMap(), toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
					if (navigableWorkspace != null) {
						return toVector(navigableWorkspace.getAccessedFrom());
					}
				}
				return itemEntity.getLocationComponent().getWorldOrParentPosition();
			}
		}
		return null;
	}
}
