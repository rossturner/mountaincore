package technology.rocketjump.saul.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.ai.combat.CreatureCombat;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.ai.memory.MemoryType;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.items.ProjectileBehaviour;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.entities.components.creature.StatusComponent;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;
import technology.rocketjump.saul.entities.model.physical.creature.body.*;
import technology.rocketjump.saul.entities.model.physical.creature.body.organs.OrganDamageEffect;
import technology.rocketjump.saul.entities.model.physical.creature.body.organs.OrganDamageLevel;
import technology.rocketjump.saul.entities.model.physical.creature.status.*;
import technology.rocketjump.saul.entities.model.physical.furniture.EntityDestructionCause;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;
import static technology.rocketjump.saul.entities.FireMessageHandler.blackenedColor;
import static technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartDamageLevel.BrokenBones;
import static technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartDamageLevel.Destroyed;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

@Singleton
public class CombatMessageHandler implements Telegraph, GameContextAware {

	private static final int DAMAGE_TO_DESTROY_FURNITURE = 10;
	private final MessageDispatcher messageDispatcher;
	private final ItemEntityFactory itemEntityFactory;
	private GameContext gameContext;

	@Inject
	public CombatMessageHandler(MessageDispatcher messageDispatcher, ItemEntityFactory itemEntityFactory) {
		this.messageDispatcher = messageDispatcher;
		this.itemEntityFactory = itemEntityFactory;

		messageDispatcher.addListener(this, MessageType.MAKE_ATTACK_WITH_WEAPON);
		messageDispatcher.addListener(this, MessageType.COMBAT_PROJECTILE_REACHED_TARGET);
		messageDispatcher.addListener(this, MessageType.APPLY_ATTACK_DAMAGE);
		messageDispatcher.addListener(this, MessageType.CREATURE_DAMAGE_APPLIED);
		messageDispatcher.addListener(this, MessageType.CREATURE_ORGAN_DAMAGE_APPLIED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MAKE_ATTACK_WITH_WEAPON: {
				handleAttackWithWeapon((CombatAttackMessage) msg.extraInfo);
				return true;
			}
			case MessageType.COMBAT_PROJECTILE_REACHED_TARGET: {
				handleProjectileImpact((CombatAttackMessage) msg.extraInfo);
				return true;
			}
			case MessageType.APPLY_ATTACK_DAMAGE: {
				applyAttackDamage((CombatAttackMessage) msg.extraInfo);
				return true;
			}
			case MessageType.CREATURE_DAMAGE_APPLIED: {
				applyDamageToCreature((CreatureDamagedMessage) msg.extraInfo);
				return true;
			}
			case MessageType.CREATURE_ORGAN_DAMAGE_APPLIED: {
				applyOrganDamageToCreature((CreatureOrganDamagedMessage) msg.extraInfo);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void handleAttackWithWeapon(CombatAttackMessage attackMessage) {
		CreatureCombat attackerCombat = new CreatureCombat(attackMessage.attackerEntity);

		boolean isRangedAttack = attackerCombat.getWeaponRangeAsInt() > 1 && attackerCombat.getEquippedWeapon().getRequiresAmmoType() != null;

		if (isRangedAttack) {
			// create ongoing effect of arrow moving towards target with rotation set
			if (attackMessage.ammoAttributes != null) {
				createProjectile(attackMessage);
				if (attackerCombat.getEquippedWeapon().getFireWeaponSoundAsset() != null) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
							attackerCombat.getEquippedWeapon().getFireWeaponSoundAsset(), attackMessage.attackerEntity
					));
				}
			}
		} else {
			// is a melee attack
			boolean attackHits = !isBlinded(attackMessage.attackerEntity);
			if (attackHits) {
				applyAttackDamage(attackMessage);
			}
			triggerHitOrMissSound(attackMessage, attackHits);
		}
	}

	private void triggerHitOrMissSound(CombatAttackMessage attackMessage, boolean attackHits) {
		if (attackHits) {
			if (attackMessage.weaponAttack.getWeaponHitSoundAsset() != null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						attackMessage.weaponAttack.getWeaponHitSoundAsset(), attackMessage.defenderEntity
				));
			}
		} else {
			if (attackMessage.weaponAttack.getWeaponMissSoundAsset() != null) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						attackMessage.weaponAttack.getWeaponMissSoundAsset(), attackMessage.defenderEntity
				));
			}
		}
	}

	private void handleProjectileImpact(CombatAttackMessage attackMessage) {
		boolean attackHits = !isBlinded(attackMessage.attackerEntity);
		if (attackHits) {
			applyAttackDamage(attackMessage);
		}
		triggerHitOrMissSound(attackMessage, attackHits);
	}

	private void createProjectile(CombatAttackMessage attackMessage) {
		Vector2 attackerLocation = attackMessage.attackerEntity.getLocationComponent().getWorldOrParentPosition();
		ItemEntityAttributes ammoAttributes = attackMessage.ammoAttributes.clone();
		ammoAttributes.setItemPlacement(ItemPlacement.PROJECTILE);
		Entity projectileEntity = itemEntityFactory.create(ammoAttributes, toGridPoint(attackerLocation), false, gameContext);
		projectileEntity.getLocationComponent().setWorldPosition(attackerLocation, false);
		ProjectileBehaviour projectileBehaviour = new ProjectileBehaviour();
		projectileBehaviour.setAttackerEntity(attackMessage.attackerEntity);
		projectileBehaviour.setDefenderEntity(attackMessage.defenderEntity);
		projectileBehaviour.setWeaponAttack(attackMessage.weaponAttack);
		projectileBehaviour.init(projectileEntity, messageDispatcher, gameContext);
		projectileEntity.replaceBehaviourComponent(projectileBehaviour);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, projectileEntity);
	}

	private boolean isBlinded(Entity attackerEntity) {
		StatusComponent statusComponent = attackerEntity.getComponent(StatusComponent.class);
		return statusComponent.contains(Blinded.class) || statusComponent.contains(TemporaryBlinded.class);
	}

	private void applyAttackDamage(CombatAttackMessage attackMessage) {
		int damageAmount = attackMessage.weaponAttack.getMinDamage() + gameContext.getRandom().nextInt(
				attackMessage.weaponAttack.getMaxDamage() - attackMessage.weaponAttack.getMinDamage()
		);
		damageAmount = scaleDamageByWeaponQuality(damageAmount, attackMessage.weaponAttack.getWeaponQuality());

		if (attackMessage.weaponAttack.isModifiedByStrength()) {
			damageAmount += getStrengthModifier(attackMessage.attackerEntity);
		}

		CombatDamageType damageType = attackMessage.weaponAttack.getDamageType();

		// reduce by target's damage reduction

		if (attackMessage.defenderEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes defenderAttributes) {
			CreatureCombat defenderCombat = new CreatureCombat(attackMessage.defenderEntity);
			damageAmount -= defenderCombat.getDamageReduction(damageType);

			if (attackMessage.attackerEntity.getType().equals(EntityType.CREATURE)) {
				MemoryComponent memoryComponent = attackMessage.defenderEntity.getOrCreateComponent(MemoryComponent.class);
				Memory memory = new Memory(MemoryType.ATTACKED_BY_CREATURE, gameContext.getGameClock());
				memory.setRelatedEntityId(attackMessage.attackerEntity.getId());
				memoryComponent.addShortTerm(memory, gameContext.getGameClock());
			}

			if (attackMessage.defenderEntity.getBehaviourComponent() instanceof CreatureBehaviour defenderBehaviour) {
				defenderBehaviour.getCombatBehaviour().attackedInCombat(attackMessage.attackerEntity);
			}

			if (damageAmount <= 0) {
				return;
			}

			if (canUseDefensePool(attackMessage)) {
				damageAmount = reduceDamageWithDefensePool(damageAmount, attackMessage.defenderEntity);
			}

			if (damageAmount > 0) {
				BodyPart impactedBodyPart = defenderAttributes.getBody().randomlySelectPartBasedOnSize(gameContext.getRandom());
				BodyPartDamage currentDamage = defenderAttributes.getBody().getDamage(impactedBodyPart);
				Optional<BodyPartOrgan> impactedOrgan = impactedBodyPart.rollToHitOrgan(gameContext.getRandom(), currentDamage);

				if (impactedOrgan.isPresent()) {
					BodyPartOrgan targetOrgan = impactedOrgan.get();
					OrganDamageLevel currentOrganDamage = defenderAttributes.getBody().getOrganDamage(impactedBodyPart, targetOrgan);
					damageAmount += currentOrganDamage.furtherDamageModifier;
					OrganDamageLevel newOrganDamage = OrganDamageLevel.getForDamageAmount(damageAmount);
					if (newOrganDamage.isGreaterThan(currentOrganDamage)) {
						defenderAttributes.getBody().setOrganDamage(impactedBodyPart, targetOrgan, newOrganDamage);
						messageDispatcher.dispatchMessage(MessageType.CREATURE_ORGAN_DAMAGE_APPLIED, new CreatureOrganDamagedMessage(
								attackMessage.defenderEntity, attackMessage.attackerEntity, impactedBodyPart, targetOrgan, newOrganDamage
						));
					}
				} else {
					// impacted with body part only
					damageAmount += currentDamage.getDamageLevel().furtherDamageModifier;
					BodyPartDamageLevel newDamageLevel = BodyPartDamageLevel.getForDamageAmount(damageAmount);
					if (newDamageLevel.isGreaterThan(currentDamage.getDamageLevel())) {
						defenderAttributes.getBody().setDamage(impactedBodyPart, newDamageLevel);
						messageDispatcher.dispatchMessage(MessageType.CREATURE_DAMAGE_APPLIED, new CreatureDamagedMessage(
								attackMessage.defenderEntity, attackMessage.attackerEntity, impactedBodyPart, newDamageLevel
						));

						if (newDamageLevel.equals(Destroyed)) {
							bodyPartDestroyed(impactedBodyPart, defenderAttributes.getBody(), attackMessage.defenderEntity, attackMessage.attackerEntity);
						}
					}
				}

				// TODO change knockback mechanics incl. when damage impacts defense pool only
//				Vector2 knockbackVector = attackMessage.defenderEntity.getLocationComponent().getWorldOrParentPosition().cpy().sub(
//						attackMessage.attackerEntity.getLocationComponent().getWorldOrParentPosition()
//				).nor().scl(damageAmount / 4f);
//				attackMessage.defenderEntity.getBehaviourComponent().getSteeringComponent().setKnockback(knockbackVector);
			}

		} else if (attackMessage.defenderEntity.getType().equals(EntityType.FURNITURE)) {
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) attackMessage.defenderEntity.getPhysicalEntityComponent().getAttributes();
			attributes.setDamageAmount(attributes.getDamageAmount() + damageAmount);

			if (attributes.getDamageAmount() > DAMAGE_TO_DESTROY_FURNITURE) {
				messageDispatcher.dispatchMessage(MessageType.DAMAGE_FURNITURE, new FurnitureDamagedMessage(
						attackMessage.defenderEntity, EntityDestructionCause.TANTRUM, null,
						blackenedColor(gameContext.getRandom()), blackenedColor(gameContext.getRandom())
				));
			}
		} else {
			Logger.warn("TODO: Damage application to non-creature entities");
		}
	}

	/**
	 * Don't use defense pool if attack is from behind or defender is stunned/asleep
	 */
	private boolean canUseDefensePool(CombatAttackMessage attackMessage) {
		if (attackMessage.defenderEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			if (creatureBehaviour.isStunned()) {
				return false;
			}
		}

		if (attackMessage.defenderEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes) {
			if (!creatureEntityAttributes.getConsciousness().equals(Consciousness.AWAKE)) {
				return false;
			}
		}

		EntityAssetOrientation defenderOrientation = EntityAssetOrientation.fromFacingTo8Directions(attackMessage.defenderEntity.getLocationComponent().getFacing());
		EntityAssetOrientation attackerRelativeToDefender = EntityAssetOrientation.fromFacingTo8Directions(attackMessage.attackerEntity.getLocationComponent().getWorldOrParentPosition().cpy()
				.sub(attackMessage.defenderEntity.getLocationComponent().getWorldOrParentPosition())
				.nor());

		// This ! operator might be confusing, but it's simpler to describe which orientations aren't covered
		return !getOrientationsOppositeTo(defenderOrientation).contains(attackerRelativeToDefender);
	}

	public static List<EntityAssetOrientation> getOrientationsOppositeTo(EntityAssetOrientation facingOrientation) {
		return switch (facingOrientation) {
			case UP -> List.of(DOWN_LEFT, DOWN, DOWN_RIGHT);
			case UP_RIGHT -> List.of(LEFT, DOWN_LEFT, DOWN);
			case RIGHT -> List.of(UP_LEFT, LEFT, DOWN_LEFT);
			case DOWN_RIGHT -> List.of(LEFT, UP_LEFT, UP);
			case DOWN -> List.of(UP_LEFT, UP, UP_RIGHT);
			case DOWN_LEFT -> List.of(UP, UP_RIGHT, RIGHT);
			case LEFT -> List.of(UP_RIGHT, RIGHT, DOWN_RIGHT);
			case UP_LEFT -> List.of(RIGHT, DOWN_RIGHT, DOWN);
		};
	}

	private int reduceDamageWithDefensePool(int damageAmount, Entity defenderEntity) {
		CombatStateComponent defenderCombatState = defenderEntity.getComponent(CombatStateComponent.class);
		int defenderDefensePool = defenderCombatState.getDefensePool();
		if (damageAmount > defenderDefensePool) {
			damageAmount = damageAmount - defenderDefensePool;
			defenderCombatState.setDefensePool(0);
		} else {
			defenderCombatState.setDefensePool(defenderDefensePool - damageAmount);
			damageAmount = 0;
		}
		return damageAmount;
	}

	private void applyDamageToCreature(CreatureDamagedMessage message) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) message.targetCreature.getPhysicalEntityComponent().getAttributes();
		StatusComponent statusComponent = message.targetCreature.getComponent(StatusComponent.class);

		switch (message.damageLevel) {
			case None:
				return;
			case Bruised:
				if (attributes.getRace().getFeatures().getBlood() == null) {
					// This creature does not have blood, so it is not affected by bruised
					attributes.getBody().setDamage(message.impactedBodyPart, BodyPartDamageLevel.None);
					return;
				}
			case Bleeding:
				if (attributes.getRace().getFeatures().getBlood() == null) {
					// This creature does not have blood, so it is not affected by bleeding
					attributes.getBody().setDamage(message.impactedBodyPart, BodyPartDamageLevel.None);
					return;
				} else {
					statusComponent.apply(new Bleeding());
				}
				break;
			case BrokenBones:
				if (attributes.getRace().getFeatures().getBones() == null) {
					// This creature does not have bones, so it is unaffected
					attributes.getBody().setDamage(message.impactedBodyPart, BodyPartDamageLevel.None);
					return;
				}
				break;
		}

		if (gameContext.getRandom().nextFloat() < message.damageLevel.chanceToCauseStun) {
			applyStun(message.targetCreature);
		}

		if (gameContext.getRandom().nextFloat() < message.damageLevel.chanceToGoUnconscious) {
			statusComponent.apply(new KnockedUnconscious());
		}

		if (message.damageLevel.equals(BrokenBones) || message.damageLevel.equals(Destroyed)) {
			statusComponent.apply(new MovementImpaired());
		}
	}

	private void applyStun(Entity targetCreature) {
		if (targetCreature.getBehaviourComponent() instanceof CreatureBehaviour behaviour) {
			behaviour.applyStun(gameContext.getRandom());
		}
		// else probably already dead or else inanimate
	}

	private void applyOrganDamageToCreature(CreatureOrganDamagedMessage message) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) message.targetEntity.getPhysicalEntityComponent().getAttributes();
		StatusComponent statusComponent = message.targetEntity.getComponent(StatusComponent.class);

		List<BodyPartOrgan> otherOrgansOfType = new ArrayList<>();
		for (BodyPart bodyPart : attributes.getBody().getAllBodyParts()) {
			for (BodyPartOrgan organForBodyPart : bodyPart.getPartDefinition().getOrgans()) {
				if (message.impactedOrgan.getOrganDefinition().equals(organForBodyPart.getOrganDefinition()) &&
						message.impactedOrgan.getDiscriminator() != organForBodyPart.getDiscriminator() &&
						!attributes.getBody().getOrganDamage(bodyPart, organForBodyPart).equals(OrganDamageLevel.DESTROYED)) {
					otherOrgansOfType.add(organForBodyPart);
				}
			}
		}

		boolean finalOrganInstance = otherOrgansOfType.isEmpty();

		Map<OrganDamageLevel, OrganDamageEffect> damageEffectMap = finalOrganInstance ?
				message.impactedOrgan.getOrganDefinition().getDamage().getFinalInstance() :
				message.impactedOrgan.getOrganDefinition().getDamage().getOther();

		OrganDamageEffect organDamageEffect = damageEffectMap.get(message.organDamageLevel);
		if (organDamageEffect != null) {
			switch (organDamageEffect) {
				case DEAD:
					messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
							new CreatureDeathMessage(message.targetEntity, DeathReason.CRITICAL_ORGAN_DAMAGE));
					break;
				case BLINDED:
					statusComponent.apply(new Blinded());
					break;
				case STUNNED:
					applyStun(message.targetEntity);
					break;
				case BLEEDING:
					statusComponent.apply(new Bleeding());
					break;
				case SUFFOCATION:
					messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
							new CreatureDeathMessage(message.targetEntity, DeathReason.SUFFOCATION));
					break;
				case VISION_IMPAIRED:
					statusComponent.apply(new TemporaryBlinded());
					break;
				case INTERNAL_BLEEDING:
					statusComponent.apply(new InternalBleeding());
					break;
				default:
					Logger.error("Unrecognised " + OrganDamageEffect.class.getSimpleName() + ": ");
			}
		}
	}

	private void bodyPartDestroyed(BodyPart impactedBodyPart, Body body, Entity targetEntity, Entity aggressorEntity) {
		for (BodyPartOrgan organ : impactedBodyPart.getPartDefinition().getOrgans()) {
			if (!body.getOrganDamage(impactedBodyPart, organ).equals(OrganDamageLevel.DESTROYED)) {
				body.setOrganDamage(impactedBodyPart, organ, OrganDamageLevel.DESTROYED);
				messageDispatcher.dispatchMessage(MessageType.CREATURE_ORGAN_DAMAGE_APPLIED, new CreatureOrganDamagedMessage(
						targetEntity, aggressorEntity, impactedBodyPart, organ, OrganDamageLevel.DESTROYED
				));
			}
		}

		for (String childPartName : impactedBodyPart.getPartDefinition().getChildParts()) {
			String[] split = childPartName.split("-");
			BodyPartDiscriminator childDiscriminator = null;
			if (split.length > 1) {
				childDiscriminator = EnumUtils.getEnum(BodyPartDiscriminator.class, split[0]);
				childPartName = split[1];
			}
			BodyPartDefinition childPartDefinition = body.getBodyStructure().getPartDefinitionByName(childPartName).orElse(null);
			if (childDiscriminator == null) {
				childDiscriminator = impactedBodyPart.getDiscriminator();
			}
			final BodyPartDiscriminator finalChildDiscriminator = childDiscriminator;

			body.getAllBodyParts()
					.stream().filter(b -> b.getPartDefinition().equals(childPartDefinition) && b.getDiscriminator() == finalChildDiscriminator)
					.forEach(b -> {
						if (!body.getDamage(b).getDamageLevel().equals(Destroyed)) {
							bodyPartDestroyed(b, body, targetEntity, aggressorEntity);
						}
					});
		}

	}

	private int getStrengthModifier(Entity attackerEntity) {
		if (attackerEntity.getType().equals(EntityType.CREATURE)) {
			CreatureEntityAttributes attributes = (CreatureEntityAttributes) attackerEntity.getPhysicalEntityComponent().getAttributes();
			float strength = attributes.getStrength();
			return getAbilityScoreModifier(Math.round(strength));
		}
		return 0;
	}

	public static int getAbilityScoreModifier(int score) {
		return (score / 3) - 3;
	}

	private int scaleDamageByWeaponQuality(int damageAmount, ItemQuality weaponQuality) {
		return Math.round((float) damageAmount * weaponQuality.combatMultiplier);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
