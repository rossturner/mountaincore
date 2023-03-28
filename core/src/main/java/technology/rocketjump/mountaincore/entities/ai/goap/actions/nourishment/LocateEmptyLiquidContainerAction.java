package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;

public class LocateEmptyLiquidContainerAction extends AbstractLocateLiquidContainerAction {

	public LocateEmptyLiquidContainerAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected void requestHaulingAllocation(ItemType itemType) {
		RequestHaulingAllocationMessage message = new RequestHaulingAllocationMessage(
				parent.parentEntity, parent.parentEntity.getLocationComponent().getWorldOrParentPosition(),
				itemType, null, false, 1,
				null, this);
		parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, message);
	}

}
