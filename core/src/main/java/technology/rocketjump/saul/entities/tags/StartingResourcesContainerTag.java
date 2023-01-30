package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class StartingResourcesContainerTag extends Tag {

	public static final String TAG_NAME = "STARTING_RESOURCES_CONTAINER";

	@Override
	public String getTagName() {
		return TAG_NAME;
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return getCapacity() >= 0;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
	}

	public int getCapacity() {
		return Integer.parseInt(getArgs().get(0));
	}
}
