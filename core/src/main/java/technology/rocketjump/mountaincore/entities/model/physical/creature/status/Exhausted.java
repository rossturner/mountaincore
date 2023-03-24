package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.ai.goap.EntityNeed;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.components.creature.NeedsComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class Exhausted extends StatusEffect {

	public Exhausted() {
		super(Death.class, 56.0, DeathReason.EXHAUSTION, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
		if (happinessComponent != null) {
			// Might be an animal with no happiness component
			happinessComponent.add(HappinessComponent.HappinessModifier.VERY_TIRED);
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		if (needsComponent == null) {
			return true;
		} else {
			return needsComponent.getValue(EntityNeed.SLEEP) > 20;
		}
	}

	@Override
	public String getI18Key() {
		return "STATUS.EXHAUSTED";
	}

}
