package technology.rocketjump.saul.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.combat.model.WeaponAttack;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.item.AmmoType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
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
import static technology.rocketjump.saul.entities.model.EntityType.ITEM;

public class AttackCreatureCombatAction extends CombatAction implements ParticleRequestMessage.ParticleCreationCallback {

	private static final float MAX_ATTACK_DURATION = 4f;
	private float timeUntilAttack;
	private float totalAttackDuration;
	private float attackDurationElapsed; // once timeUntilAttack has elapsed, this counts how far through the attack "animation" we are
	private boolean attackMade; // done halfway through attackDuration - actual calculation of hit/miss with weapon
	private ParticleEffectInstance effectInstance; // transient
	private Long overrideTarget;

	public AttackCreatureCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		Long opponentId = getOpponentId();
		if (opponentId == null) {
			completed = true;
			return;
		}
		Entity opponentEntity = gameContext.getEntities().get(opponentId);
		if (opponentEntity == null) {
			completed = true;
			return;
		}

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

			if (attackDurationElapsed >= totalAttackDuration || attackDurationElapsed > MAX_ATTACK_DURATION) {
				showWeapon();
				completed = true;
			}
		}
	}

	private void beginAttack(GameContext gameContext, MessageDispatcher messageDispatcher) {
		CreatureCombat combatStats = new CreatureCombat(parentEntity);

		if (isInRangeOfOpponent(parentEntity, gameContext.getEntities().get(getOpponentId()))) {
			WeaponInfo weapon = combatStats.getEquippedWeapon();
			if (weapon.getAnimatedEffectType() != null) {
				totalAttackDuration = weapon.getAnimatedEffectType().getOverrideDuration();
				ParticleRequestMessage requestMessage = new ParticleRequestMessage(weapon.getAnimatedEffectType(), Optional.of(parentEntity), Optional.empty(), this);
				if (combatStats.getEquippedWeaponAttributes() != null) {
					requestMessage.setOverrideColor(combatStats.getEquippedWeaponAttributes().getPrimaryMaterial().getColor());
				}
				messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, requestMessage);
				hideWeapon();
			} else {
				// No animation associated, just trigger attack right now
				triggerAttack(gameContext.getEntities().get(getOpponentId()), messageDispatcher);
				completed = true;
			}
		} else {
			// Not in range of opponent anymore! Just do nothing for the rest of this round I guess
			completed = true;
		}
	}

	private void triggerAttack(Entity targetedEntity, MessageDispatcher messageDispatcher) {
		CreatureCombat creatureCombat = new CreatureCombat(parentEntity);
		ItemEntityAttributes ammoAttributes = decrementAmmoFromInventory(creatureCombat.getEquippedWeapon().getRequiresAmmoType(), messageDispatcher);
		messageDispatcher.dispatchMessage(MessageType.MAKE_ATTACK_WITH_WEAPON, new CombatAttackMessage(
				parentEntity, targetedEntity, new WeaponAttack(creatureCombat.getEquippedWeapon(), creatureCombat.getEquippedWeaponQuality()),
				ammoAttributes));
		attackMade = true;
	}

	private Long getOpponentId() {
		return overrideTarget != null ? overrideTarget : parentEntity.getComponent(CombatStateComponent.class).getTargetedOpponentId();
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

	private ItemEntityAttributes decrementAmmoFromInventory(AmmoType requiredAmmoType, MessageDispatcher messageDispatcher) {
		if (requiredAmmoType == null) {
			return null;
		}
		Optional<InventoryComponent.InventoryEntry> inventoryEntry = parentEntity.getComponent(InventoryComponent.class).getInventoryEntries()
				.stream()
				.filter(e -> e.entity.getType().equals(ITEM) &&
						requiredAmmoType.equals(((ItemEntityAttributes) e.entity.getPhysicalEntityComponent().getAttributes()).getItemType().getIsAmmoType()))
				.findFirst();
		if (inventoryEntry.isPresent()) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryEntry.get().entity.getPhysicalEntityComponent().getAttributes();

			ItemEntityAttributes cloned = attributes.clone();
			cloned.setQuantity(1);

			attributes.setQuantity(attributes.getQuantity() - 1);
			if (attributes.getQuantity() <= 0) {
				messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, inventoryEntry.get().entity);
			}

			return cloned;
		}
		return null;
	}

	public float getTimeUntilAttack() {
		return timeUntilAttack;
	}

	public void setTimeUntilAttack(float timeUntilAttack) {
		this.timeUntilAttack = timeUntilAttack;
	}

	public void setOverrideTarget(Long overrideTarget) {
		this.overrideTarget = overrideTarget;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("timeUntilAttack", timeUntilAttack);
		asJson.put("totalAttackDuration", totalAttackDuration);
		asJson.put("attackDurationElapsed", attackDurationElapsed);
		if (attackMade) {
			asJson.put("attackMade", true);
		}
		if (overrideTarget != null) {
			asJson.put("overrideTarget", overrideTarget);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.timeUntilAttack = asJson.getFloatValue("timeUntilAttack");
		this.totalAttackDuration = asJson.getFloatValue("totalAttackDuration");
		this.attackDurationElapsed = asJson.getFloatValue("attackDurationElapsed");
		this.attackMade = asJson.getBooleanValue("attackMade");
		this.overrideTarget = asJson.getLong("overrideTarget");
	}

	@Override
	public void particleCreated(ParticleEffectInstance instance) {
		this.effectInstance = instance;
	}

}
