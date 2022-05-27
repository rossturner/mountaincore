package technology.rocketjump.saul.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.components.humanoid.HappinessComponent;
import technology.rocketjump.saul.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;
import technology.rocketjump.saul.gamecontext.GameContext;

public class DyingOfThirst extends StatusEffect {

	public DyingOfThirst() {
		super(Death.class, 12.0, DeathReason.DEHYDRATION);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.DYING_OF_THIRST);
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
		return "STATUS.DYING_OF_THIRST";
	}

}
