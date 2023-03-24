package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class TemporaryBlinded extends StatusEffect {

	public TemporaryBlinded() {
		super(null, 0.3, null, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

	@Override
	public String getI18Key() {
		return "STATUS.TEMPORARILY_BLINDED";
	}

}
