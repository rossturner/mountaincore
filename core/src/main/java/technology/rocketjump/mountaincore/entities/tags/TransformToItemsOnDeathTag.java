package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemCreationRequestMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransformToItemsOnDeathTag extends Tag {
    @Override
    public String getTagName() {
        return "TRANSFORM_TO_ITEMS_ON_DEATH";
    }

    @Override
    public boolean isValid(TagProcessingUtils tagProcessingUtils) {
        ItemTypeDictionary itemTypeDictionary = tagProcessingUtils.itemTypeDictionary;
        if (args.size() % 2 != 0) {
            return false;
        }

        for (int i = 0; i < args.size(); i+=2) {
            String itemTypeName = args.get(i);
            String quantity = args.get(i + 1);
            ItemType itemType = itemTypeDictionary.getByName(itemTypeName);
            if (itemType == null || !StringUtils.isNumeric(quantity)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
        //Do nothing? - Should be easier handled in EntityMessageHandler for now
    }

    public List<Entity> createItemEntities(MessageDispatcher messageDispatcher, ItemTypeDictionary itemTypeDictionary, Map<GameMaterialType, GameMaterial> preferredMaterials) {
        List<Entity> entities = new ArrayList<>(args.size() / 2);
        for (int i = 0; i < args.size(); i+=2) {
            String itemTypeName = args.get(i);
            String quantity = args.get(i + 1);
            final ItemType itemType = itemTypeDictionary.getByName(itemTypeName);


            EntityCreatedCallback callback = entity -> {
                EntityAttributes attributes = entity.getPhysicalEntityComponent().getAttributes();
                if (attributes instanceof ItemEntityAttributes itemEntityAttributes) {
                    itemEntityAttributes.setQuantity(Integer.parseInt(quantity));
                    for (GameMaterialType materialType : itemType.getMaterialTypes()) {
                        if (preferredMaterials.containsKey(materialType)) {
                            itemEntityAttributes.setMaterial(preferredMaterials.get(materialType));
                        }
                    }
                }
                entities.add(entity);
            };
            messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(itemType, callback));
        }

        return entities;
    }
}
