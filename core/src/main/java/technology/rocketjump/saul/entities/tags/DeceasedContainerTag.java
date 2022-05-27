package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class DeceasedContainerTag extends Tag {

	@Override
	public String getTagName() {
		return "DECEASED_CONTAINER";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.isEmpty() || args.size() == 1;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing, parsed later
	}

}