package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

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
