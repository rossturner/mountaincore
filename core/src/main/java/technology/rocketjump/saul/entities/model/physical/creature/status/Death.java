package technology.rocketjump.saul.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.CreatureDeathMessage;

public class Death extends StatusEffect {

	private DeathReason deathReason;

	public Death() {
		super(null, null, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (parentEntity.getType().equals(EntityType.CREATURE)) {
			messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(parentEntity, deathReason));
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

	@Override
	public String getI18Key() {
		return "STATUS.DEATH";
	}

	public void setDeathReason(DeathReason deathReason) {
		this.deathReason = deathReason;
	}

	public DeathReason getDeathReason() {
		return deathReason;
	}
}
