package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.PlannedTrade;
import technology.rocketjump.saul.entities.ai.goap.actions.PickUpEntityAction;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;

public class PickUpPaymentAction extends PickUpEntityAction {

	public PickUpPaymentAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		PlannedTrade plannedTrade = parent.getPlannedTrade();
		if (plannedTrade != null) {
			MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());

			Entity targetItem = gameContext.getEntity(plannedTrade.getPaymentItemAllocation().getTargetItemEntityId());
			if (targetItem != null) {
				pickUpItemEntity(gameContext, currentTile, targetItem, plannedTrade.getPaymentItemAllocation());
			}
		}

		if (completionType == null) {
			completionType = CompletionType.FAILURE;
		}
	}
}
