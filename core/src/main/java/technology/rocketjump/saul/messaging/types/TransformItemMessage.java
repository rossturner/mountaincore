package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;

public class TransformItemMessage {

	public final Entity itemEntity;
	public final ItemType transformToItemType;

	public TransformItemMessage(Entity itemEntity, ItemType transformToItemType) {
		this.itemEntity = itemEntity;
		this.transformToItemType = transformToItemType;
	}

}
