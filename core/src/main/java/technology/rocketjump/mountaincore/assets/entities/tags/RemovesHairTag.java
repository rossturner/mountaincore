package technology.rocketjump.mountaincore.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessingUtils;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class RemovesHairTag extends Tag {

	@Override
	public String getTagName() {
		return "REMOVES_HAIR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		// This tag has no args
		return args == null || args.isEmpty();
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		entity.getPhysicalEntityComponent().getTypeMap().remove(tagProcessingUtils.entityAssetTypeDictionary.getByName("CREATURE_HORNS"));
		entity.getPhysicalEntityComponent().getTypeMap().remove(tagProcessingUtils.entityAssetTypeDictionary.getByName("CREATURE_HAIR"));
		entity.getPhysicalEntityComponent().getTypeMap().remove(tagProcessingUtils.entityAssetTypeDictionary.getByName("HAIR_OUTLINE"));
	}

}
