package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class ItemMaterialSelectionMessage {

	public final ItemType itemType;
	public final ItemMaterialSelectionCallback callback;

	public ItemMaterialSelectionMessage(ItemType itemType, ItemMaterialSelectionCallback callback) {
		this.itemType = itemType;
		this.callback = callback;
	}

	public interface ItemMaterialSelectionCallback {

		void materialFound(GameMaterial material);

	}
}
