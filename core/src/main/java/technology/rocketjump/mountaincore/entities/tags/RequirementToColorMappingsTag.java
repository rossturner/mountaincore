package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

import java.util.Map;

public class RequirementToColorMappingsTag extends Tag {

	@Override
	public String getTagName() {
		return "REQUIREMENT_TO_COLOR_MAPPINGS";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.size() % 3 == 0;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing, this is special case tag applied by ConstructionMessageHandler
	}

	public void apply(Entity createdFurnitureEntity, Map<Long, Entity> itemsRemovedFromConstruction, ItemTypeDictionary itemTypeDictionary) {
		FurnitureEntityAttributes targetAttributes = (FurnitureEntityAttributes) createdFurnitureEntity.getPhysicalEntityComponent().getAttributes();

		for (int configCursor = 0; configCursor < args.size(); configCursor += 3) {
			ItemType itemType = itemTypeDictionary.getByName(args.get(configCursor));
			ColoringLayer sourceColorLayer = ColoringLayer.valueOf(args.get(configCursor + 1));
			ColoringLayer targetColorLayer = ColoringLayer.valueOf(args.get(configCursor + 2));

			if (itemType == null) {
				Logger.error("Unrecognised item type in " + this.getClass().getSimpleName() + " with name " + args.get(configCursor));
			} else if (!EnumUtils.isValidEnum(ColoringLayer.class, args.get(configCursor + 1)) ||
					!EnumUtils.isValidEnum(ColoringLayer.class, args.get(configCursor + 2))) {
				Logger.error("Unrecognised " + ColoringLayer.class.getSimpleName() + " in " + this.getClass().getSimpleName() + " with name " + args.get(configCursor + 1));
			} else {
				Entity matchingEntity = DecorationFromInputTag.getMatching(itemType, itemsRemovedFromConstruction);
				if (matchingEntity == null) {
					Logger.warn("Could not find matching entity with item type " + itemType.getItemTypeName() + " in " + this.getClass().getSimpleName());
				} else {
					ItemEntityAttributes sourceAttributes = (ItemEntityAttributes) matchingEntity.getPhysicalEntityComponent().getAttributes();
					targetAttributes.setColor(targetColorLayer, sourceAttributes.getColor(sourceColorLayer));
				}
			}
		}

	}
}
