package technology.rocketjump.mountaincore.entities.behaviour.effects;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.particles.custom_libgdx.ShaderEffect;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectInstance;

import static technology.rocketjump.mountaincore.entities.behaviour.effects.FireEffectBehaviour.FireContinuationAction.CONTINUE_BURNING;
import static technology.rocketjump.mountaincore.entities.behaviour.effects.FireEffectBehaviour.FireContinuationAction.DIE_OUT;

public class FireEffectBehaviour extends BaseOngoingEffectBehaviour {


	@Override
	public FireEffectBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FireEffectBehaviour cloned = new FireEffectBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		ParticleEffectInstance particleEffectInstance = currentParticleEffect.get();

		if (particleEffectInstance != null && particleEffectInstance.getWrappedInstance() instanceof ShaderEffect) {
			ShaderEffect wrappedInstance = (ShaderEffect) particleEffectInstance.getWrappedInstance();
			if (OngoingEffectState.ACTIVE.equals(state)) {
				float alpha = wrappedInstance.getTint().a;
				if (alpha < 1) {
					alpha = Math.min(stateDuration, 1f);
				}
				wrappedInstance.getTint().a = alpha;
			} else if (OngoingEffectState.FADING.equals(state)) {
				float fadeDuration = attributes.getType().getStates().get(OngoingEffectState.FADING).getDuration();
				float alpha =  (fadeDuration - stateDuration) / fadeDuration;
				wrappedInstance.getTint().a = alpha;
			}
		}

	}

	public void setToFade() {
		this.state = OngoingEffectState.FADING;
		this.stateDuration = 0f;
	}

	@Override
	public boolean shouldNotificationApply(GameContext gameContext) {
		return gameContext != null && gameContext.getMapEnvironment().getCurrentWeather().getLightningStrikesPerHour() == null;
	}


	@Override
	protected void nextState() {
		switch (state) {
			case STARTING:
				this.state = OngoingEffectState.ACTIVE;
				break;
			case ACTIVE:
				FireContinuationAction continuation = rollForContinuation();
				switch (continuation) {
					case SPREAD_TO_OTHER_TILES:
						messageDispatcher.dispatchMessage(MessageType.SPREAD_FIRE_FROM_LOCATION, parentEntity.getLocationComponent().getWorldOrParentPosition());
						this.state = OngoingEffectState.ACTIVE; // reset to active state again
						break;
					case CONTINUE_BURNING:
						this.state = OngoingEffectState.ACTIVE;
						break;
					case CONSUME_PARENT:
						Entity containerEntity = parentEntity.getLocationComponent().getContainerEntity();
						if (containerEntity != null) {
							AttachedEntitiesComponent attachedEntitiesComponent = containerEntity.getComponent(AttachedEntitiesComponent.class);
							if (attachedEntitiesComponent != null) {
								attachedEntitiesComponent.remove(parentEntity); //remove fire so that fire tags don't copy over
							}
							messageDispatcher.dispatchMessage(MessageType.CONSUME_ENTITY_BY_FIRE, containerEntity);
						} else {
							messageDispatcher.dispatchMessage(MessageType.CONSUME_TILE_BY_FIRE, parentEntity.getLocationComponent().getWorldOrParentPosition());
						}
						this.state = OngoingEffectState.FADING;
						break;
					case DIE_OUT:
						this.state = OngoingEffectState.FADING;
						break;
				}
				break;
			case FADING:
				this.state = null;
				break;
		}

		this.stateDuration = 0f;
	}

	private FireContinuationAction rollForContinuation() {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (parentTile != null && parentTile.getRoof().getState().equals(TileRoofState.OPEN) && gameContext.getMapEnvironment().getCurrentWeather().getChanceToExtinguishFire() != null) {
			if (gameContext.getRandom().nextFloat() < gameContext.getMapEnvironment().getCurrentWeather().getChanceToExtinguishFire()) {
				return DIE_OUT;
			}
		}

		float roll = gameContext.getRandom().nextFloat();
		for (FireContinuationAction continuationAction : FireContinuationAction.values()) {
			if (roll <= continuationAction.chance) {
				return continuationAction;
			} else {
				roll -= continuationAction.chance;
			}
		}
		return CONTINUE_BURNING;
	}

	public enum FireContinuationAction {

		CONTINUE_BURNING(0.4f),
		SPREAD_TO_OTHER_TILES(0.3f),
		CONSUME_PARENT(0.3f),
		DIE_OUT(1f);

		private final float chance;

		FireContinuationAction(float chance) {
			this.chance = chance;
		}
	}

}
