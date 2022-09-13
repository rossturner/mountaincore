package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class ItemMaterialSelectionMessage {

	public final ItemType itemType;
	public final int minimumQuantity;
	public final ItemMaterialSelectionCallback callback;

	public ItemMaterialSelectionMessage(ItemType itemType, int minimumQuantity, ItemMaterialSelectionCallback callback) {
		this.itemType = itemType;
		this.minimumQuantity = minimumQuantity;
		this.callback = callback;
	}

	public interface ItemMaterialSelectionCallback {

		void materialFound(GameMaterial material);

	}
}
