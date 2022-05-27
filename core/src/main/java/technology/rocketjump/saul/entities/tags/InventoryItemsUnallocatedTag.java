package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class InventoryItemsUnallocatedTag extends Tag {
	@Override
	public String getTagName() {
		return "INVENTORY_ITEMS_UNALLOCATED";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true; // No args
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		InventoryComponent inventoryComponent = entity.getOrCreateComponent(InventoryComponent.class);
		inventoryComponent.setItemsUnallocated(true);
	}

}
