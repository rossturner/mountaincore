package technology.rocketjump.saul.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.behaviour.furniture.ProductionImportFurnitureBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;

public class ProductionImportTag extends Tag {

	@Override
	public String getTagName() {
		return "PRODUCTION_IMPORT";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return Integer.parseInt(args.get(0)) > 0;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getType().equals(EntityType.FURNITURE)) {
			if (!(entity.getBehaviourComponent() instanceof ProductionImportFurnitureBehaviour)) {
				ProductionImportFurnitureBehaviour importBehaviour = new ProductionImportFurnitureBehaviour();
				importBehaviour.setMaxNumItemStacks(Integer.parseInt(args.get(0)));

				messageDispatcher.dispatchMessage(MessageType.CHANGE_ENTITY_BEHAVIOUR, new MessageType.ChangeEntityBehaviourMessage(entity, importBehaviour));
			}
		} else {
			Logger.error("Attempting to apply " + getTagName() + " to non-furniture entity");
		}
	}

}
