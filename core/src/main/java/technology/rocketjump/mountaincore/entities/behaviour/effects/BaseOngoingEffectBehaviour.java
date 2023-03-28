package technology.rocketjump.mountaincore.entities.behaviour.effects;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.audio.model.ActiveSoundEffect;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SteeringComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.FurnitureParticleEffectsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.JobTarget;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ParticleRequestMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundStopMessage;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectInstance;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static technology.rocketjump.mountaincore.entities.behaviour.effects.BaseOngoingEffectBehaviour.OngoingEffectState.*;

public class BaseOngoingEffectBehaviour implements BehaviourComponent, Destructible {

	protected Entity parentEntity;
	protected MessageDispatcher messageDispatcher;
	protected GameContext gameContext;

	protected final AtomicReference<ParticleEffectInstance> currentParticleEffect = new AtomicReference<>(null);
	protected ActiveSoundEffect activeSoundEffect;

	protected OngoingEffectState state = OngoingEffectState.STARTING;
	protected float stateDuration;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
	}

	@Override
	public BaseOngoingEffectBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		BaseOngoingEffectBehaviour cloned = new BaseOngoingEffectBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime) {
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		Entity containerEntity = parentEntity.getLocationComponent().getContainerEntity();
		if (state.equals(STARTING) && containerEntity != null && containerEntity.getType().equals(EntityType.CREATURE)) {
			nextState();
		}

		stateDuration += deltaTime;
		if (stateDuration > attributes.getType().getStates().get(state).getDuration()) {
			nextState();
		}
		if (this.state == null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);
			return;
		}

		ParticleEffectInstance currentEffectInstance = currentParticleEffect.get();

		if (currentEffectInstance != null && !currentEffectInstance.isActive()) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, currentEffectInstance);
			currentParticleEffect.set(null);
			currentEffectInstance = null;
		}

		if (currentEffectInstance == null && state.equals(ACTIVE)) {
			ParticleEffectType particleEffectType = attributes.getType().getParticleEffectType();
			Entity entityToAttachParticleTo = parentEntity;
			if (containerEntity != null) {
				entityToAttachParticleTo = containerEntity;
			}

			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
					particleEffectType, Optional.of(entityToAttachParticleTo), Optional.empty(), (particle) -> {
				particle.getWrappedInstance().setTint(HexColors.get(attributes.getType().getInitialColor()));
				currentParticleEffect.set(particle);
			}));
		}

		if (attributes.getType().getPlaySoundAsset() != null && !STARTING.equals(state)) {
			if (activeSoundEffect != null) {
				if (activeSoundEffect.completed()) {
					stopSound(parentEntity, messageDispatcher);
				}
			}

			if (activeSoundEffect == null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						attributes.getType().getPlaySoundAsset(), parentEntity, (sound) -> {
					activeSoundEffect = sound;
				}));
			}
		}

		FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
		if (particleEffectsComponent != null) {
			Optional<JobTarget> particleTarget = Optional.empty();
			if (containerEntity != null) {
				particleTarget = Optional.of(new JobTarget(containerEntity));
			} else if (parentEntity.getLocationComponent().getWorldPosition() != null) {
				particleTarget = Optional.of(new JobTarget(gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition())));
			}
			particleEffectsComponent.triggerProcessingEffects(particleTarget);
		}

	}

	@Override
	public void updateWhenPaused() {

	}

	protected void nextState() {
		switch (state) {
			case STARTING:
				this.state = ACTIVE;
				break;
			case ACTIVE:
				this.state = FADING;
				break;
			case FADING:
				this.state = null;
				break;
		}
		this.stateDuration = 0f;
	}

	public boolean shouldNotificationApply(GameContext gameContext) {
		return true;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		ParticleEffectInstance effectInstance = currentParticleEffect.get();
		if (effectInstance != null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, effectInstance);
			currentParticleEffect.set(null);
		}

		if (activeSoundEffect != null) {
			stopSound(parentEntity, messageDispatcher);
		}

		FurnitureParticleEffectsComponent particleEffectsComponent = parentEntity.getComponent(FurnitureParticleEffectsComponent.class);
		if (particleEffectsComponent != null) {
			particleEffectsComponent.releaseParticles();
		}
	}

	private void stopSound(Entity parentEntity, MessageDispatcher messageDispatcher) {
		messageDispatcher.dispatchMessage(MessageType.REQUEST_STOP_SOUND_LOOP,
				new RequestSoundStopMessage(activeSoundEffect.getAsset(), parentEntity.getId()));
		activeSoundEffect = null;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {

	}


	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return false;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	public enum OngoingEffectState {

		STARTING,
		ACTIVE,
		FADING

	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!state.equals(STARTING)) {
			asJson.put("state", state);
		}
		asJson.put("stateDuration", stateDuration);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.state = EnumParser.getEnumValue(asJson, "state", OngoingEffectState.class, STARTING);
		this.stateDuration = asJson.getFloatValue("stateDuration");
	}
}
