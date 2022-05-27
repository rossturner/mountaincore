package technology.rocketjump.saul.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;

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
		EntityAssetType hairType = tagProcessingUtils.entityAssetTypeDictionary.getByName("HUMANOID_HAIR");
		entity.getPhysicalEntityComponent().getTypeMap().remove(hairType);
	}
}
