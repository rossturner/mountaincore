package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

public class RequestHaulingAllocationMessage {

	public final Entity requestingEntity;
	public final Vector2 requesterPosition;
	public final ItemType requiredItemType;
	public final Class<? extends Tag> requiredItemTypeTag;
	public final GameMaterial requiredMaterial;
	public final GameMaterial requiredContainedLiquid;
	public final boolean includeFromFurniture;
	public final ItemAllocationCallback allocationCallback;
	public final Integer maxAmountRequired;

	public RequestHaulingAllocationMessage(Entity requestingEntity, Vector2 requesterPosition, ItemType requiredItemType, GameMaterial requiredMaterial,
										   boolean includeFromFurniture, Integer maxAmountRequired, GameMaterial requiredContainedLiquid, ItemAllocationCallback allocationCallback) {
		this(requestingEntity, requesterPosition, requiredItemType, null, requiredMaterial, includeFromFurniture, maxAmountRequired, requiredContainedLiquid, allocationCallback);
	}
	public RequestHaulingAllocationMessage(Entity requestingEntity, Vector2 requesterPosition, Class<? extends Tag> requiredItemTypeTag, GameMaterial requiredMaterial,
										   boolean includeFromFurniture, Integer maxAmountRequired, GameMaterial requiredContainedLiquid, ItemAllocationCallback allocationCallback) {
		this(requestingEntity, requesterPosition, null, requiredItemTypeTag, requiredMaterial, includeFromFurniture, maxAmountRequired, requiredContainedLiquid, allocationCallback);
	}
	private RequestHaulingAllocationMessage(Entity requestingEntity, Vector2 requesterPosition, ItemType requiredItemType, Class<? extends Tag> requiredItemTypeTag, GameMaterial requiredMaterial,
										   boolean includeFromFurniture, Integer maxAmountRequired, GameMaterial requiredContainedLiquid, ItemAllocationCallback allocationCallback) {
		this.requestingEntity = requestingEntity;
		this.requesterPosition = requesterPosition;
		this.requiredItemType = requiredItemType;
		this.requiredItemTypeTag = requiredItemTypeTag;
		this.requiredMaterial = requiredMaterial;
		this.includeFromFurniture = includeFromFurniture;
		this.maxAmountRequired = maxAmountRequired;
		this.requiredContainedLiquid = requiredContainedLiquid;
		this.allocationCallback = allocationCallback;
	}

	public interface ItemAllocationCallback {
		void allocationFound(HaulingAllocation haulingAllocation);
	}
}
