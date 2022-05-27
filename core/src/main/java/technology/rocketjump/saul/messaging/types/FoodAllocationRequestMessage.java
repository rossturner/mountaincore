package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.cooking.model.FoodAllocation;
import technology.rocketjump.saul.entities.model.Entity;

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
