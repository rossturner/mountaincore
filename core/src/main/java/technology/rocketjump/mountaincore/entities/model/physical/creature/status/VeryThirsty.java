package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.ai.goap.EntityNeed;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.components.creature.NeedsComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class VeryThirsty extends StatusEffect {

	public VeryThirsty() {
		super(DyingOfThirst.class, 18.0, DeathReason.DEHYDRATION, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
		if (happinessComponent != null) {
			happinessComponent.add(HappinessComponent.HappinessModifier.VERY_THIRSTY);
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		if (needsComponent == null) {
			return true;
		} else {
			return needsComponent.getValue(EntityNeed.DRINK) > 1;
		}
	}

	@Override
	public String getI18Key() {
		return "STATUS.VERY_THIRSTY";
	}

}
