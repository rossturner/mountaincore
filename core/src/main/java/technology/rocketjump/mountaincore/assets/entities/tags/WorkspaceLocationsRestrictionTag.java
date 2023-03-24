package technology.rocketjump.mountaincore.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessingUtils;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class WorkspaceLocationsRestrictionTag extends Tag {
	@Override
	public String getTagName() {
		return "WORKSPACE_LOCATIONS_MUST_BE_INSIDE_ROOM";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing
	}
}
