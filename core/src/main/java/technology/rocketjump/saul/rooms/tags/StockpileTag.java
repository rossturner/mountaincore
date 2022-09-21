package technology.rocketjump.saul.rooms.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.tags.InventoryItemsUnallocatedTag;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.production.FurnitureStockpile;
import technology.rocketjump.saul.production.StockpileSettings;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.StockpileComponent;

public class StockpileTag extends Tag {
	@Override
	public String getTagName() {
		return "STOCKPILE";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;//todo validate
//		return args.size() == 0;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		room.createComponent(StockpileComponent.class, tagProcessingUtils.messageDispatcher);
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (EntityType.FURNITURE == entity.getType() && entity.getComponent(FurnitureStockpileComponent.class) == null) {
			new InventoryItemsUnallocatedTag().apply(entity, tagProcessingUtils, messageDispatcher, gameContext);

			int maxQuantity = 0;
			if (args.size() > 0) {
				maxQuantity = Integer.parseInt(args.get(0));
			}
			FurnitureStockpile furnitureStockpile = new FurnitureStockpile();
			furnitureStockpile.setMaxQuantity(maxQuantity);

			StockpileSettings stockpileSettings = new StockpileSettings();
			for (int i = 1; i < args.size(); i++) {
				String stockpileGroupName = args.get(i);
				stockpileSettings.addRestriction(stockpileGroupName);
			}

			FurnitureStockpileComponent component = new FurnitureStockpileComponent(stockpileSettings, furnitureStockpile);
			component.init(entity, messageDispatcher, gameContext);
			entity.addComponent(component);
		}
	}
}
