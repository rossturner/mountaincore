package technology.rocketjump.saul.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.tags.ExtraFurnitureHitpointsTag;
import technology.rocketjump.saul.assets.entities.tags.FlatDamageReductionTag;
import technology.rocketjump.saul.combat.model.WeaponAttack;
import technology.rocketjump.saul.entities.ai.combat.*;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.ai.memory.MemoryType;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.items.ProjectileBehaviour;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
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
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;
import static technology.rocketjump.saul.entities.FireMessageHandler.blackenedColor;
import static technology.rocketjump.saul.entities.behaviour.creature.CombatBehaviour.getOpponentsInMelee;
import static technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartDamageLevel.Destroyed;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

@Singleton
public class CombatMessageHandler implements Telegraph, GameContextAware {

	private static final int DAMAGE_TO_DESTROY_FURNITURE = 10;
	private static final int XP_GAIN_PER_COMBAT_ROUND = 1;
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
		messageDispatcher.addListener(this, MessageType.TRIGGER_ATTACK_OF_OPPORTUNITY);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MAKE_ATTACK_WITH_WEAPON: {
				CombatAttackMessage attackMessage = (CombatAttackMessage) msg.extraInfo;
				handleAttackWithWeapon(attackMessage);
				awardCombatExperience(attackMessage);
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
			case MessageType.TRIGGER_ATTACK_OF_OPPORTUNITY: {
				Entity fleeingEntity = (Entity) msg.extraInfo;
				triggerAttackOfOpportunity(fleeingEntity);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void awardCombatExperience(CombatAttackMessage attackMessage) {
		for (Entity combatant : List.of(attackMessage.attackerEntity, attackMessage.defenderEntity)) {
			SkillsComponent skillsComponent = combatant.getComponent(SkillsComponent.class);
			if (skillsComponent != null) {
				CreatureCombat creatureCombat = new CreatureCombat(combatant);
				skillsComponent.experienceGained(XP_GAIN_PER_COMBAT_ROUND, creatureCombat.getEquippedWeapon().getCombatSkill());
			}
		}
	}

	private void triggerAttackOfOpportunity(Entity fleeingEntity) {
		for (Entity opponentEntity : getOpponentsInMelee(fleeingEntity, gameContext)) {
			CombatStateComponent opponentCombatState = opponentEntity.getComponent(CombatStateComponent.class);
			if (opponentCombatState != null && !opponentCombatState.isAttackOfOpportunityMadeThisRound()) {
				if (opponentEntity.getBehaviourComponent() instanceof CreatureBehaviour opponentBehaviour) {
					opponentBehaviour.getCombatBehaviour().makeAttackOfOpportunity(fleeingEntity);
				}
			}
		}
	}

	private void handleAttackWithWeapon(CombatAttackMessage attackMessage) {
		CreatureCombat attackerCombat = new CreatureCombat(attackMessage.attackerEntity);

		if (attackerCombat.getEquippedWeapon().isRanged()) {
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

		// reduce by target's damage reduction

		if (attackMessage.defenderEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes defenderAttributes) {
			damageAmount = adjustDamageAmount(damageAmount, attackMessage);

			if (attackMessage.attackerEntity.getType().equals(EntityType.CREATURE)) {
				MemoryComponent memoryComponent = attackMessage.defenderEntity.getOrCreateComponent(MemoryComponent.class);
				Memory memory = new Memory(MemoryType.ATTACKED_BY_CREATURE, gameContext.getGameClock());
				memory.setRelatedEntityId(attackMessage.attackerEntity.getId());
				memoryComponent.addShortTerm(memory, gameContext.getGameClock());
			}

			if (attackMessage.defenderEntity.getBehaviourComponent() instanceof CreatureBehaviour defenderBehaviour) {
				defenderBehaviour.getCombatBehaviour().attackedInCombat(attackMessage.attackerEntity);
			}

			handleKnockback(attackMessage);

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
								attackMessage.defenderEntity, impactedBodyPart, targetOrgan, newOrganDamage,
								attackMessage.attackerEntity));
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
							if(impactedBodyPart.getPartDefinition().getName().equals(defenderAttributes.getBody().getBodyStructure().getRootPartName())) {
								messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH,
										new CreatureDeathMessage(attackMessage.defenderEntity, DeathReason.EXTENSIVE_INJURIES, attackMessage.attackerEntity));
							}
						}
					}
				}

			}

		} else if (attackMessage.defenderEntity.getType().equals(EntityType.FURNITURE)) {
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) attackMessage.defenderEntity.getPhysicalEntityComponent().getAttributes();

			damageAmount = calculateFurnitureDamage(attackMessage.defenderEntity, damageAmount);

			attributes.setDamageAmount(attributes.getDamageAmount() + damageAmount);

			if (attributes.getDamageAmount() > damageToDestroyFurniture(attackMessage.defenderEntity)) {
				messageDispatcher.dispatchMessage(MessageType.DAMAGE_FURNITURE, new FurnitureDamagedMessage(
						attackMessage.defenderEntity, EntityDestructionCause.COMBAT_DAMAGE, null,
						blackenedColor(gameContext.getRandom()), blackenedColor(gameContext.getRandom())
				));
			}
		} else {
			Logger.warn("TODO: Damage application to non-creature entities");
		}
	}

	private int calculateFurnitureDamage(Entity defenderEntity, int damageAmount) {
		FlatDamageReductionTag damageReductionTag = defenderEntity.getTag(FlatDamageReductionTag.class);
		if (damageReductionTag != null) {
			damageAmount -= damageReductionTag.getValue();
		}
		return Math.max(0, damageAmount);
	}

	private int damageToDestroyFurniture(Entity defenderEntity) {
		int damageToDestroy = DAMAGE_TO_DESTROY_FURNITURE;
		ExtraFurnitureHitpointsTag extraHitpointsTag = defenderEntity.getTag(ExtraFurnitureHitpointsTag.class);
		if (extraHitpointsTag != null) {
			damageToDestroy += extraHitpointsTag.getValue();
		}
		return damageToDestroy;
	}

	public static int adjustDamageAmount(int currentDamageAmount, CombatAttackMessage attackMessage) {
		Entity defenderEntity = attackMessage.defenderEntity;
		WeaponAttack weaponAttack = attackMessage.weaponAttack;
		CombatDamageType damageType = weaponAttack.getDamageType();
		CreatureCombat creatureCombat = new CreatureCombat(defenderEntity);

		int armorNegation = attackMessage.weaponAttack.getArmorNegation();
		int damageReduction = creatureCombat.getDamageReduction(damageType);

		if (damageReduction >= 0) {
			return currentDamageAmount - Math.max(0, damageReduction - armorNegation);
		} else {
			return currentDamageAmount - Math.min(0, damageReduction + armorNegation);
		}
	}

	private void handleKnockback(CombatAttackMessage attackMessage) {
		if (attackMessage.defenderEntity.getBehaviourComponent() instanceof CreatureBehaviour defenderBehaviour && shouldApplyKnockback(defenderBehaviour)) {
			GridPoint2 defenderTilePosition = toGridPoint(attackMessage.defenderEntity.getLocationComponent().getWorldOrParentPosition());
			List<CompassDirection> pushbackDirections = CompassDirection.fromNormalisedVector(
					attackMessage.defenderEntity.getLocationComponent().getWorldOrParentPosition().cpy()
							.sub(attackMessage.attackerEntity.getLocationComponent().getWorldOrParentPosition()).nor()
			).withNeighbours();

			List<MapTile> navigablePushbackTiles = new ArrayList<>();
			for (CompassDirection direction : pushbackDirections) {
				MapTile tile = gameContext.getAreaMap().getTile(
						defenderTilePosition.x + direction.getXOffset(),
						defenderTilePosition.y + direction.getYOffset()
				);
				if (tile != null && tile.isNavigable(attackMessage.defenderEntity)) {
					navigablePushbackTiles.add(tile);
				}
			}

			if (navigablePushbackTiles.isEmpty()) {
				return;
			} // else can definitely push back somewhere

			List<MapTile> emptyPushbackTiles = navigablePushbackTiles.stream()
					.filter(tile ->
							tile.getEntities().stream()
									.noneMatch(entity -> {
										CombatStateComponent combatStateComponent = entity.getComponent(CombatStateComponent.class);
										if (combatStateComponent == null || !combatStateComponent.isInCombat() || combatStateComponent.getHeldLocation() == null) {
											return false;
										} else {
											return combatStateComponent.getHeldLocation().equals(toGridPoint(entity.getLocationComponent().getWorldOrParentPosition()));
										}
									})
					).toList();

			if (!emptyPushbackTiles.isEmpty()) {
				MapTile targetTile = emptyPushbackTiles.get(gameContext.getRandom().nextInt(emptyPushbackTiles.size()));
				triggerKnockback(defenderTilePosition, targetTile, attackMessage);
			} else {
				// all knockback tiles have a combatant in them, pick one and knock back that combatant also
				MapTile targetTile = navigablePushbackTiles.get(gameContext.getRandom().nextInt(navigablePushbackTiles.size()));
				triggerKnockback(defenderTilePosition, targetTile, attackMessage);

				targetTile.getEntities().forEach(entity -> {
					CombatStateComponent combatStateComponent = entity.getComponent(CombatStateComponent.class);
					if (combatStateComponent != null && combatStateComponent.isInCombat() && combatStateComponent.getHeldLocation() != null &&
							combatStateComponent.getHeldLocation().equals(targetTile.getTilePosition())) {
						handleKnockback(new CombatAttackMessage(attackMessage.defenderEntity, entity, null, null));
					}
				});
			}
		}
	}

	private void triggerKnockback(GridPoint2 fromTile, MapTile targetTile, CombatAttackMessage message) {
		if (message.attackerEntity.getBehaviourComponent() instanceof CreatureBehaviour attackerBehaviour) {
			if (attackerBehaviour.getCombatBehaviour().getCurrentAction() instanceof AttackCreatureCombatAction attackAction) {
				attackAction.setFollowUpKnockbackTo(fromTile);
			}
		}
		if (message.defenderEntity.getBehaviourComponent() instanceof CreatureBehaviour defenderBehaviour) {
			CombatStateComponent defenderCombatState = message.defenderEntity.getComponent(CombatStateComponent.class);
			if (defenderCombatState.isInCombat()) {
				CombatAction currentDefenderAction = defenderBehaviour.getCombatBehaviour().getCurrentAction();
				if (!(currentDefenderAction instanceof KnockBackCombatAction)) {
					defenderBehaviour.getCombatBehaviour().immediateKnockbackTo(targetTile.getTilePosition());
				}
			} else {
				defenderBehaviour.getCombatBehaviour().setPendingKnockback(targetTile.getTilePosition());
			}
		}
	}

	private boolean shouldApplyKnockback(CreatureBehaviour defenderBeahviour) {
		return gameContext.getRandom().nextBoolean() && !(defenderBeahviour.getCombatBehaviour().getCurrentAction() instanceof FleeFromCombatAction);
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
		for (BodyPart bodyPart : attributes.getBody().getAllWorkingBodyParts()) {
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
							new CreatureDeathMessage(message.targetEntity, DeathReason.CRITICAL_ORGAN_DAMAGE, message.aggressorEntity));
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
							new CreatureDeathMessage(message.targetEntity, DeathReason.SUFFOCATION, message.aggressorEntity));
					break;
				case VISION_IMPAIRED:
					statusComponent.apply(new TemporaryBlinded());
					break;
				case INTERNAL_BLEEDING:
					statusComponent.apply(new InternalBleeding(message.aggressorEntity));
					break;
				default:
					Logger.error("Unrecognised " + OrganDamageEffect.class.getSimpleName() + ": ");
			}
		}
	}

	public void bodyPartDestroyed(BodyPart impactedBodyPart, Body body, Entity targetEntity, Entity attackerEntity) {
		StatusComponent statusComponent = targetEntity.getOrCreateComponent(StatusComponent.class);
		for (BodyPart child : body.iterateRecursively(impactedBodyPart)) {
			body.setDamage(child, Destroyed);
			for (BodyPartOrgan organ : child.getPartDefinition().getOrgans()) {
				if (!body.getOrganDamage(child, organ).equals(OrganDamageLevel.DESTROYED)) {
					body.setOrganDamage(child, organ, OrganDamageLevel.DESTROYED);
					messageDispatcher.dispatchMessage(MessageType.CREATURE_ORGAN_DAMAGE_APPLIED, new CreatureOrganDamagedMessage(
							targetEntity, child, organ, OrganDamageLevel.DESTROYED,
							attackerEntity));
				}
			}

			BodyPartFunction function = child.getPartDefinition().getFunction();
			if (function != null) {
				switch (function) {
					case MAIN_HAND -> statusComponent.apply(new LossOfMainHand());
					case OFF_HAND -> statusComponent.apply(new LossOfOffHand());
					case MOVEMENT -> statusComponent.apply(new MovementImpaired());
				}
			}
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
