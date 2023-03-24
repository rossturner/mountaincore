package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.misc.VectorUtils;

public class GoToFoodLocationAction extends GoToLocationAction {
	public GoToFoodLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getFoodAllocation() == null || parent.getFoodAllocation().getTargetEntity() == null) {
			return null;
		}

		Entity targetEntity = parent.getFoodAllocation().getTargetEntity();

		switch (parent.getFoodAllocation().getType()) {
			case LIQUID_CONTAINER: {
				return getNavigableWorkspace(targetEntity, gameContext);
			}
			case FURNITURE_INVENTORY:
			case LOOSE_ITEM: {
				Entity containerEntity = targetEntity.getLocationComponent().getContainerEntity();
				if (containerEntity != null) {
					// Item is in a container
					if (containerEntity.getType().equals(EntityType.FURNITURE)) {
						return getNavigableWorkspace(containerEntity, gameContext);
					} else if (containerEntity.getId() == parent.parentEntity.getId()) {
						// Item is in parent's inventory
						return null;
					} else {
						Logger.error("Not yet implemented: Selecting destination for item within non-furniture container in " + this.getSimpleName());
						return null;
					}
				} else {
					// Not in a container
					return targetEntity.getLocationComponent().getWorldPosition();
				}
			}
			case REQUESTER_INVENTORY: {
				this.completionType = CompletionType.SUCCESS;
				return null;
			}
			default: {
				Logger.error("Not yet implemented: " + this.getSimpleName() + " for food allocation type " + parent.getFoodAllocation().getType());
				return null;
			}
		}
	}

	private Vector2 getNavigableWorkspace(Entity targetEntity, GameContext gameContext) {
		FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getNearestNavigableWorkspace(targetEntity, gameContext.getAreaMap(), VectorUtils.toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
		if (navigableWorkspace == null) {
			// Could not navigate to any workspaces
			return null;
		} else {
			return VectorUtils.toVector(navigableWorkspace.getAccessedFrom());
		}
	}

}
