package technology.rocketjump.saul.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.CombatAttackMessage;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.saul.entities.behaviour.creature.CombatBehaviour.isInRangeOfOpponent;

public class AttackCreatureCombatAction extends CombatAction implements ParticleRequestMessage.ParticleCreationCallback {

	private float timeUntilAttack;
	private float totalAttackDuration;
	private float attackDurationElapsed; // once timeUntilAttack has elapsed, this counts how far through the attack "animation" we are
	private boolean attackMade; // done halfway through attackDuration - actual calculation of hit/miss with weapon
	private ParticleEffectInstance effectInstance; // transient

	public AttackCreatureCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		Entity opponentEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
		if (opponentEntity != null) {
			// Face towards opponent
			Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
			Vector2 opponentPosition = opponentEntity.getLocationComponent().getWorldOrParentPosition();
			parentEntity.getLocationComponent().setFacing(opponentPosition.cpy().sub(parentPosition));

			if (timeUntilAttack > 0) {
				timeUntilAttack -= deltaTime;
				if (timeUntilAttack <= 0) {
					beginAttack(gameContext, messageDispatcher);
				}
			}

			if (totalAttackDuration > 0 && attackDurationElapsed < totalAttackDuration) {
				attackDurationElapsed += deltaTime;
				if (attackDurationElapsed > totalAttackDuration / 2 && !attackMade) {
					triggerAttack(opponentEntity, messageDispatcher);
				}

				if (attackDurationElapsed >= totalAttackDuration) {
					showWeapon();
				}
			}
		}
	}

	private void beginAttack(GameContext gameContext, MessageDispatcher messageDispatcher) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		CreatureCombatStats combatStats = new CreatureCombatStats(parentEntity);

		if (isInRangeOfOpponent(parentEntity, gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId()))) {
			WeaponInfo weapon = combatStats.getEquippedWeapon();
			if (weapon.getAnimatedEffectType() != null) {
				totalAttackDuration = weapon.getAnimatedEffectType().getOverrideDuration();
				ParticleRequestMessage requestMessage = new ParticleRequestMessage(weapon.getAnimatedEffectType(), Optional.of(parentEntity), Optional.empty(), this);
				messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, requestMessage);
				hideWeapon();
			} else {
				// No animation associated, just trigger attack right now
				triggerAttack(gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId()), messageDispatcher);
			}
		} else {
			// Not in range of opponent anymore! Just do nothing for the rest of this round I guess
		}
	}

	private void triggerAttack(Entity targetedEntity, MessageDispatcher messageDispatcher) {
		messageDispatcher.dispatchMessage(MessageType.MAKE_ATTACK_WITH_WEAPON, new CombatAttackMessage(parentEntity, targetedEntity));
		attackMade = true;
	}

	@Override
	public void interrupted(MessageDispatcher messageDispatcher) {
		showWeapon();
		if (effectInstance != null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, effectInstance);
		}
	}

	@Override
	public boolean completesInOneRound() {
		return true;
	}

	@Override
	public void onRoundCompletion() {
		super.onRoundCompletion();
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		combatStateComponent.setHasInitiative(false);
	}

	private void showWeapon() {
		EquippedItemComponent equippedItemComponent = parentEntity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null) {
			equippedItemComponent.setHideMainHandItem(false);
		}
	}

	private void hideWeapon() {
		EquippedItemComponent equippedItemComponent = parentEntity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null) {
			equippedItemComponent.setHideMainHandItem(true);
		}
	}

	public float getTimeUntilAttack() {
		return timeUntilAttack;
	}

	public void setTimeUntilAttack(float timeUntilAttack) {
		this.timeUntilAttack = timeUntilAttack;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("timeUntilAttack", timeUntilAttack);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.timeUntilAttack = asJson.getFloatValue("timeUntilAttack");
	}

	@Override
	public void particleCreated(ParticleEffectInstance instance) {
		this.effectInstance = instance;
	}
}
