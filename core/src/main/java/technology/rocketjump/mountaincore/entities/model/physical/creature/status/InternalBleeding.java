package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.CreatureDeathMessage;

public class InternalBleeding extends StatusEffect {

	private static final float CHANCE_OF_DEATH_ON_TICK = 1f / 35f;

	//Default for reflection based construction, if required
	public InternalBleeding() {
		this(null);
	}

	public InternalBleeding(Entity inflictedBy) {
		super(null, 3.0, null, inflictedBy);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (gameContext.getRandom().nextFloat() < CHANCE_OF_DEATH_ON_TICK) {
			messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
					new CreatureDeathMessage(parentEntity, DeathReason.INTERNAL_BLEEDING, inflictedBy));
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

	@Override
	public String getI18Key() {
		return "STATUS.INTERNAL_BLEEDING";
	}

}
