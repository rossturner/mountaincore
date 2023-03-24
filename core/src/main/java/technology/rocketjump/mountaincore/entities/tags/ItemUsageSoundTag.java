package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class ItemUsageSoundTag extends Tag {

	@Override
	public String getTagName() {
		return "USAGE_SOUND";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return tagProcessingUtils.soundAssetDictionary.getByName(getSoundAssetName()) != null;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
	}

	public String getSoundAssetName() {
		return args.get(0);
	}
}
