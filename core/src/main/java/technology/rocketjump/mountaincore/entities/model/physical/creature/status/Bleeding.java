package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ParticleRequestMessage;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectInstance;

import java.util.Optional;

public class Bleeding extends StatusEffect implements ParticleRequestMessage.ParticleCreationCallback {

	private ParticleEffectInstance particleEffectInstance;

	public Bleeding() {
		super(null, 4.0, null, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
		if (happinessComponent != null) {
			happinessComponent.add(HappinessComponent.HappinessModifier.BLEEDING);
		}

		if (particleEffectInstance == null) {
			ParticleRequestMessage message = new ParticleRequestMessage(
					"Bleeding", Optional.of(parentEntity), Optional.empty(), this
			);
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
			if (attributes.getRace().getFeatures().getBlood() != null) {
				message.setOverrideColor(attributes.getRace().getFeatures().getBlood().getColor());
			}
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, message);
		}
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false; // removed by hoursUntilNextStage expiry
	}

	@Override
	public String getI18Key() {
		return "STATUS.BLEEDING";
	}

	@Override
	public void particleCreated(ParticleEffectInstance instance) {
		this.particleEffectInstance = instance;
	}
}
