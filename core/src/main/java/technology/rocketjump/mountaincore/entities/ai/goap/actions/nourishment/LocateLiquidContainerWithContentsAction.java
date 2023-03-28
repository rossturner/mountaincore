package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;

import java.util.Collection;
import java.util.function.Consumer;

public class LocateLiquidContainerWithContentsAction extends AbstractLocateLiquidContainerAction {

	public LocateLiquidContainerWithContentsAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected void requestHaulingAllocation(ItemType itemType) {
		Consumer<Collection<GameMaterial>> callback = gameMaterials -> {
			for (GameMaterial gameMaterial : gameMaterials) {

				RequestHaulingAllocationMessage message = new RequestHaulingAllocationMessage(
						parent.parentEntity, parent.parentEntity.getLocationComponent().getWorldOrParentPosition(),
						itemType, null, false, 1,
						gameMaterial, this);

				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, message);

				if (completionType != null) {
					return;
				}
			}
		};
		parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_MATERIAL, new MessageType.RequestLiquidMaterialMessage(callback));


	}

}
