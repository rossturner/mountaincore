package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class ReplacementDeconstructionResourcesTag extends Tag {
	@Override
	public String getTagName() {
		return "REPLACEMENT_DESCONSTRUCTION_RESOURCES";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true; // No args
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Only used during deconstruction
	}

}
