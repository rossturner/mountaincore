package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class Poisoned extends StatusEffect {

	private static final float CHANCE_TO_REMOVE_ON_TICK = 1f / 25f;

	public Poisoned() {
		super(Death.class, 4.0, DeathReason.FOOD_POISONING, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.POISONED);
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		// Based on infrequentUpdate interval and 4.0 hours to death, there will be 27 checks for removal
		// Based on a check at 1/25 each time, ~33% chance of never removing (and therefore dying)
		return gameContext.getRandom().nextFloat() < CHANCE_TO_REMOVE_ON_TICK;
	}

	@Override
	public String getI18Key() {
		return "STATUS.POISONED";
	}

}
