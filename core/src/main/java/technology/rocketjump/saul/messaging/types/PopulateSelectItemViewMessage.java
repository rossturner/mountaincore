package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;

import java.util.function.Consumer;

public class PopulateSelectItemViewMessage {

	public final ItemSelectionCategory itemSelectionCategory;
	public final Entity requestingEntity;
	public final Consumer<Entity> callback;

	public PopulateSelectItemViewMessage(ItemSelectionCategory itemSelectionCategory, Entity requestingEntity, Consumer<Entity> callback) {
		this.itemSelectionCategory = itemSelectionCategory;
		this.requestingEntity = requestingEntity;
		this.callback = callback;
	}

	public enum ItemSelectionCategory {

		WEAPON,
		SHIELD,
		ARMOR

	}

}
