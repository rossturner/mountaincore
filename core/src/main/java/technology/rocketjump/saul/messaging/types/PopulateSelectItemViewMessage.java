package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;

import java.util.function.Consumer;

public class PopulateSelectItemViewMessage {

	public final ItemSelectionCategory itemSelectionCategory;
	public final Consumer<Entity> callback;

	public PopulateSelectItemViewMessage(ItemSelectionCategory itemSelectionCategory, Consumer<Entity> callback) {
		this.itemSelectionCategory = itemSelectionCategory;
		this.callback = callback;
	}

	public enum ItemSelectionCategory {

		WEAPON,
		SHIELD,
		ARMOR

	}

}
