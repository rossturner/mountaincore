package technology.rocketjump.mountaincore.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.ProductionImportFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessingUtils;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;

public class ProductionImportTag extends Tag {

	@Override
	public String getTagName() {
		return "PRODUCTION_IMPORT";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return Integer.parseInt(args.get(0)) > 0 &&
				tagProcessingUtils.jobTypeDictionary.getByName(args.get(1)) != null;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getType().equals(EntityType.FURNITURE)) {
			if (!(entity.getBehaviourComponent() instanceof ProductionImportFurnitureBehaviour)) {
				ProductionImportFurnitureBehaviour importBehaviour = new ProductionImportFurnitureBehaviour();
				importBehaviour.setMaxNumItemStacks(Integer.parseInt(args.get(0)));
				importBehaviour.setHaulingJobType(tagProcessingUtils.jobTypeDictionary.getByName(args.get(1)));

				messageDispatcher.dispatchMessage(MessageType.CHANGE_ENTITY_BEHAVIOUR, new MessageType.ChangeEntityBehaviourMessage(entity, importBehaviour));
			}
		} else {
			Logger.error("Attempting to apply " + getTagName() + " to non-furniture entity");
		}
	}

}
