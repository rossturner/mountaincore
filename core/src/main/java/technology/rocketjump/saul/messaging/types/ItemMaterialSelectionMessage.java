package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.util.function.Consumer;

public class ItemMaterialSelectionMessage {

	public final ItemType itemType;
	public final int minimumQuantity;
	public final Consumer<GameMaterial> callback;

	public ItemMaterialSelectionMessage(ItemType itemType, int minimumQuantity, Consumer<GameMaterial> callback) {
		this.itemType = itemType;
		this.minimumQuantity = minimumQuantity;
		this.callback = callback;
	}

}
