package technology.rocketjump.saul.rooms.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.combat.DefenseType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.tags.InventoryItemsUnallocatedTag;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.production.FurnitureStockpile;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileSettings;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.StockpileComponent;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockpileTag extends Tag {

	private static final Pattern IS_MACRO_PATTERN = Pattern.compile("IS_(ARMOUR|WEAPON)");
	private static final Pattern ITEM_TYPE_PATTERN = Pattern.compile("ItemType_(.*)");

	@Override
	public String getTagName() {
		return "STOCKPILE";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		boolean isValid = true;
		if (args.size() > 0) {
			isValid = args.get(0).matches("\\d+");
		}
		for (int i = 1; i < args.size(); i++) {
			String argument = args.get(i);
			isValid = IS_MACRO_PATTERN.matcher(argument).matches() || ITEM_TYPE_PATTERN.matcher(argument).matches();
		}
		return isValid;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		room.createComponent(StockpileComponent.class, tagProcessingUtils.messageDispatcher);
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (EntityType.FURNITURE == entity.getType() && entity.getComponent(FurnitureStockpileComponent.class) == null) {
			new InventoryItemsUnallocatedTag().apply(entity, tagProcessingUtils, messageDispatcher, gameContext);

			StockpileComponentUpdater stockpileComponentUpdater = tagProcessingUtils.stockpileComponentUpdater;


			int maxQuantity = 0;
			if (args.size() > 0) {
				maxQuantity = Integer.parseInt(args.get(0));
			}
			FurnitureStockpile furnitureStockpile = new FurnitureStockpile();
			furnitureStockpile.setMaxQuantity(maxQuantity);

			StockpileSettings stockpileSettings = new StockpileSettings();
			for (int i = 1; i < args.size(); i++) {
				String restrictionArgumentString = args.get(i);
				Matcher isMacroMatcher = IS_MACRO_PATTERN.matcher(restrictionArgumentString);
				Matcher itemTypeMatcher = ITEM_TYPE_PATTERN.matcher(restrictionArgumentString);
				Predicate<ItemType> predicate = itemType -> false;
				if (isMacroMatcher.matches()) {
					String macroName = isMacroMatcher.group(1);
					predicate = switch (macroName) {
						case "ARMOUR" -> itemType -> itemType.getDefenseInfo() != null && DefenseType.ARMOR == itemType.getDefenseInfo().getType();
						case "WEAPON" -> itemType -> itemType.getWeaponInfo() != null;
						default -> itemType -> false;
					};
				} else if (itemTypeMatcher.matches()) {
					String itemTypeName = itemTypeMatcher.group(1);
					predicate = itemType -> itemTypeName.equalsIgnoreCase(itemType.getItemTypeName());
				}
				tagProcessingUtils.itemTypeDictionary.getAll().stream()
								.filter(predicate)
								.forEach(itemType -> {
									stockpileComponentUpdater.toggleItem(stockpileSettings, itemType, true, true, true);
									stockpileSettings.addRestriction(itemType);
								});
			}

			FurnitureStockpileComponent component = new FurnitureStockpileComponent(stockpileSettings, furnitureStockpile);
			component.init(entity, messageDispatcher, gameContext);
			entity.addComponent(component);
		}
	}
}
