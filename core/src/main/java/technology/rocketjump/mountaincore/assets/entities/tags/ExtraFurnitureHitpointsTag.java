package technology.rocketjump.mountaincore.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessingUtils;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class ExtraFurnitureHitpointsTag extends Tag {

	@Override
	public String getTagName() {
		return "EXTRA_FURNITURE_HITPOINTS";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return StringUtils.isNumeric(args.get(0));
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Do nothing
	}

	public int getValue() {
		return Integer.parseInt(args.get(0));
	}

}
