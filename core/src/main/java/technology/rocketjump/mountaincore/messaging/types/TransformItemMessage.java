package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;

public class TransformItemMessage {

	public final Entity itemEntity;
	public final ItemType transformToItemType;

	public TransformItemMessage(Entity itemEntity, ItemType transformToItemType) {
		this.itemEntity = itemEntity;
		this.transformToItemType = transformToItemType;
	}

}
