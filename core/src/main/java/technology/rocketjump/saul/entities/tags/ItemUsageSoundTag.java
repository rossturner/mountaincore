package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class ItemUsageSoundTag extends Tag {

	private SoundAsset soundAsset;

	@Override
	public String getTagName() {
		return "USAGE_SOUND";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.size() == 1;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.soundAsset = tagProcessingUtils.soundAssetDictionary.getByName(args.get(0));
		if (this.soundAsset == null) {
			Logger.error("Could not find sound asset with name " + args.get(0) + " for " + getTagName());
		}
	}

	public SoundAsset getSoundAsset() {
		return soundAsset;
	}
}
