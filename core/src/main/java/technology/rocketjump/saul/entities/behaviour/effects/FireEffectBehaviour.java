package technology.rocketjump.saul.entities.behaviour.effects;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.roof.TileRoofState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.particles.custom_libgdx.ShaderEffect;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;

import static technology.rocketjump.saul.entities.behaviour.effects.BaseOngoingEffectBehaviour.OngoingEffectState.ACTIVE;
import static technology.rocketjump.saul.entities.behaviour.effects.BaseOngoingEffectBehaviour.OngoingEffectState.FADING;
import static technology.rocketjump.saul.entities.behaviour.effects.FireEffectBehaviour.FireContinuationAction.CONTINUE_BURNING;
import static technology.rocketjump.saul.entities.behaviour.effects.FireEffectBehaviour.FireContinuationAction.DIE_OUT;

public class FireEffectBehaviour extends BaseOngoingEffectBehaviour {


	@Override
	public FireEffectBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FireEffectBehaviour cloned = new FireEffectBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		super.update(deltaTime, gameContext);
		OngoingEffectAttributes attributes = (OngoingEffectAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		ParticleEffectInstance particleEffectInstance = currentParticleEffect.get();

		if (particleEffectInstance != null && particleEffectInstance.getWrappedInstance() instanceof ShaderEffect) {
			ShaderEffect wrappedInstance = (ShaderEffect) particleEffectInstance.getWrappedInstance();
			if (ACTIVE.equals(state)) {
				float alpha = wrappedInstance.getTint().a;
				if (alpha < 1) {
					alpha = Math.min(stateDuration, 1f);
				}
				wrappedInstance.getTint().a = alpha;
			} else if (FADING.equals(state)) {
				float fadeDuration = attributes.getType().getStates().get(FADING).getDuration();
				float alpha =  (fadeDuration - stateDuration) / fadeDuration;
				wrappedInstance.getTint().a = alpha;
			}
		}

	}

	public void setToFade() {
		this.state = FADING;
		this.stateDuration = 0f;
	}

	@Override
	public boolean shouldNotificationApply(GameContext gameContext) {
		return gameContext != null && gameContext.getMapEnvironment().getCurrentWeather().getLightningStrikesPerHour() == null;
	}


	@Override
	protected void nextState(GameContext gameContext) {
		switch (state) {
			case STARTING:
				this.state = ACTIVE;
				break;
			case ACTIVE:
				FireContinuationAction continuation = rollForContinuation(gameContext);
				switch (continuation) {
					case SPREAD_TO_OTHER_TILES:
						messageDispatcher.dispatchMessage(MessageType.SPREAD_FIRE_FROM_LOCATION, parentEntity.getLocationComponent().getWorldOrParentPosition());
						this.state = ACTIVE; // reset to active state again
						break;
					case CONTINUE_BURNING:
						this.state = ACTIVE;
						break;
					case CONSUME_PARENT:
						if (parentEntity.getLocationComponent().getContainerEntity() != null) {
							messageDispatcher.dispatchMessage(MessageType.CONSUME_ENTITY_BY_FIRE, parentEntity.getLocationComponent().getContainerEntity());
						} else {
							messageDispatcher.dispatchMessage(MessageType.CONSUME_TILE_BY_FIRE, parentEntity.getLocationComponent().getWorldOrParentPosition());
						}
						this.state = FADING;
						break;
					case DIE_OUT:
						this.state = FADING;
						break;
				}
				break;
			case FADING:
				this.state = null;
				break;
		}

		this.stateDuration = 0f;
	}

	private FireContinuationAction rollForContinuation(GameContext gameContext) {
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
