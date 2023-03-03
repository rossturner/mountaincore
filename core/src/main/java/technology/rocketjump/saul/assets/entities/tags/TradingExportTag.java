package technology.rocketjump.saul.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.behaviour.furniture.TradingExportFurnitureBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;

public class TradingExportTag extends Tag {

	@Override
	public String getTagName() {
		return "TRADING_EXPORT";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getType().equals(EntityType.FURNITURE)) {
			if (!(entity.getBehaviourComponent() instanceof TradingExportFurnitureBehaviour)) {
				TradingExportFurnitureBehaviour exportBehaviour = new TradingExportFurnitureBehaviour();
				exportBehaviour.setMaxNumItemStacks(Integer.parseInt(args.get(0)));
				exportBehaviour.setHaulingJobType(tagProcessingUtils.jobTypeDictionary.getByName(args.get(1)));

				messageDispatcher.dispatchMessage(MessageType.CHANGE_ENTITY_BEHAVIOUR, new MessageType.ChangeEntityBehaviourMessage(entity, exportBehaviour));
			}
		} else {
			Logger.error("Attempting to apply " + getTagName() + " to non-furniture entity");
		}
	}

}
