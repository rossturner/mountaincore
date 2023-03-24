package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.cooking.model.FoodAllocation;
import technology.rocketjump.mountaincore.entities.model.Entity;

public class FoodAllocationRequestMessage {

	public final Entity requestingEntity;
	public final FoodAllocationCallback callback;

	public FoodAllocationRequestMessage(Entity requestingEntity, FoodAllocationCallback callback) {
		this.requestingEntity = requestingEntity;
		this.callback = callback;
	}

	public interface FoodAllocationCallback {

		void foodAssigned(FoodAllocation allocation);

	}

}
