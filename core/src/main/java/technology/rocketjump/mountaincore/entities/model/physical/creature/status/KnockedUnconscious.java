package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.SleepOnFloorAction;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;

public class KnockedUnconscious extends StatusEffect {


	public KnockedUnconscious() {
		super(null, 1.0, null, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(Consciousness.AWAKE)) {
			attributes.setConsciousness(Consciousness.KNOCKED_UNCONSCIOUS);

			if (parentEntity.isJobAssignable()) {
				SleepOnFloorAction.showAsRotatedOnSide(parentEntity, gameContext);
			}

			if (parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
				if (creatureBehaviour.getCurrentGoal() != null) {
					creatureBehaviour.getCurrentGoal().setInterrupted(true);
				}
				creatureBehaviour.getCombatBehaviour().interrupted();
			}

			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false; // removed by hoursUntilNextStage expiry
	}

	@Override
	public String getI18Key() {
		return "STATUS.KNOCKED_UNCONSCIOUS";
	}

}
