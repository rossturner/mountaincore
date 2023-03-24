package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.ai.goap.EntityNeed;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.components.creature.NeedsComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class DyingOfHunger extends StatusEffect {

	public DyingOfHunger() {
		super(Death.class, 16.0, DeathReason.STARVATION, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.DYING_OF_HUNGER);
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		if (needsComponent == null) {
			return true;
		} else {
			return needsComponent.getValue(EntityNeed.FOOD) > 1;
		}
	}

	@Override
	public String getI18Key() {
		return "STATUS.DYING_OF_HUNGER";
	}

}
