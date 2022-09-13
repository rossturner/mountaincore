package technology.rocketjump.saul.assets.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.gamecontext.GameContext;

public class FlatDamageReductionTag extends Tag {

	@Override
	public String getTagName() {
		return "FLAT_DAMAGE_REDUCTION";
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
