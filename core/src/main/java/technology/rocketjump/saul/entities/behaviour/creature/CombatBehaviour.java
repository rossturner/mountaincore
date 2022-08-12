package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.NotImplementedException;
import org.reflections.ReflectionUtils;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.ai.combat.*;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.ai.goap.actions.EquipWeaponAction;
import technology.rocketjump.saul.entities.ai.goap.actions.UnequipWeaponAction;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.NeedsComponent;
import technology.rocketjump.saul.entities.components.creature.StatusComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.AggressionResponse;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.body.Body;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartDamage;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartDamageLevel;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.CombatActionChangedMessage;
import technology.rocketjump.saul.messaging.types.ParticleEffectTypeCallback;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.particles.custom_libgdx.DefensePoolBarEffect;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.particles.model.ParticleEffectType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.saul.combat.CombatMessageHandler.getOrientationsOppositeTo;
import static technology.rocketjump.saul.entities.ai.goap.Goal.NULL_GOAL;
import static technology.rocketjump.saul.entities.model.physical.creature.AggressionResponse.ATTACK;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

// Although this is coded as a Component, it is always encapsulated in CreatureBehaviour
public class CombatBehaviour implements ParentDependentEntityComponent, ParticleEffectTypeCallback,
		ParticleRequestMessage.ParticleCreationCallback, Destructible {

	private static final Double SERIOUSLY_LOW_NEED_VALUE = 10.0;
	private static final float MAX_DISTANCE_FROM_COMBAT_START = 25f;
	private static final float MAX_DISTANCE_FROM_COMBAT_START_SQUARED = MAX_DISTANCE_FROM_COMBAT_START * MAX_DISTANCE_FROM_COMBAT_START;
	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private CombatAction currentAction;
	private AttackCreatureCombatAction attackOfOpportunityAction;
	private transient ParticleEffectType defensePoolEffectType;
	private transient ParticleEffectInstance defensePoolEffect;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;

		if (currentAction != null) {
			// Required after loading from disk
			currentAction.setParentEntity(parentEntity);
		}
		if (attackOfOpportunityAction != null) {
			attackOfOpportunityAction.setParentEntity(parentEntity);
		}

		messageDispatcher.dispatchMessage(MessageType.GET_DEFENSE_POOL_EFFECT_TYPE, this);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (defensePoolEffect != null) {
			defensePoolEffect.getWrappedInstance().allowCompletion();
		}
	}


	public void onStartOfNewCombatRound() {
		parentEntity.getComponent(CombatStateComponent.class).setAttackOfOpportunityMadeThisRound(false);

		if (currentAction instanceof MoveInRangeOfTargetCombatAction && tooFarFromCombatStartingPosition()) {
			currentAction = new FleeFromCombatAction(parentEntity);
		}
	}

	/**
	 * This is expected to be called by CombatTracker for it to know what is going on in the round
	 */
	public CombatAction selectNewActionForRound(boolean stunned) {
		CombatAction previousAction = currentAction;
		if (stunned) {
			return new StunnedCombatAction(parentEntity);
		}

		CombatAction newAction;
		if (previousAction == null) {
			newAction = initialCombatAction();
		} else {
			newAction = nextCombatAction(previousAction);
		}
		this.currentAction = newAction;
		return newAction;
	}

	public CombatAction changeCombatActionAtStartOfRound() {
		// TODO something to decide to switch to the defensive if attacked by multiple opponents in melee

		// otherwise return null
		return null;
	}

	public void update(float deltaTime) throws ExitingCombatException {
		if (currentAction != null) {
			currentAction.update(deltaTime, gameContext, messageDispatcher);
			if (currentAction instanceof FleeFromCombatAction && currentAction.isCompleted()) {
				throw new ExitingCombatException();
			}
		}

		if (attackOfOpportunityAction != null) {
			attackOfOpportunityAction.update(deltaTime, gameContext, messageDispatcher);

			if (attackOfOpportunityAction.isCompleted()) {
				attackOfOpportunityAction = null;
			}
		}

		updateWhenPaused();
	}

	public void updateWhenPaused() {
		if (defensePoolEffect == null || !defensePoolEffect.isActive()) {
			if (defensePoolEffectType != null) {
				messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
						defensePoolEffectType,
						Optional.of(parentEntity),
						Optional.empty(),
						this));
			}
		}

		if (defensePoolEffect != null && defensePoolEffect.isActive()) {
			if (defensePoolEffect.getWrappedInstance() instanceof DefensePoolBarEffect defensePoolBarEffect) {
				CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
				CreatureCombat combat = new CreatureCombat(parentEntity);
				defensePoolBarEffect.setPoolPercentage((float)combatStateComponent.getDefensePool() / (float) combat.maxDefensePool());
				FactionComponent factionComponent = parentEntity.getOrCreateComponent(FactionComponent.class);
				defensePoolBarEffect.setShieldSpriteColor(factionComponent.getFaction().defensePoolShieldColor);
				defensePoolBarEffect.setProgressBarColor(factionComponent.getFaction().defensePoolBarColor);
			}
		}
	}

	private boolean tooFarFromCombatStartingPosition() {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		Vector2 startingPosition = combatStateComponent.getEnteredCombatAtPosition();
		Vector2 currentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();

		return startingPosition.dst2(currentPosition) > MAX_DISTANCE_FROM_COMBAT_START_SQUARED;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not yet implemented " + this.getClass().getSimpleName() + ".clone()");
	}

	private CombatAction initialCombatAction() {
		if (getAggressionResponse().equals(ATTACK)) {
			return attackOrDefendAgainstOpponentAction();
		} else {
			return new FleeFromCombatAction(parentEntity);
		}
	}

	private CombatAction nextCombatAction(CombatAction previousAction) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent.getTargetedOpponentId() == null) {
			if (combatStateComponent.getOpponentEntityIds().isEmpty()) {
				return new FleeFromCombatAction(parentEntity);
			} else {
				Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
				Optional<Entity> nearestOpponent = combatStateComponent.getOpponentEntityIds().stream()
						.map(entityId -> gameContext.getEntities().get(entityId))
						.filter(Objects::nonNull)
						.min(Comparator.comparingInt(e -> (int)(100f * e.getLocationComponent().getWorldOrParentPosition().dst2(parentPosition))));
				if (nearestOpponent.isPresent()) {
					combatStateComponent.setTargetedOpponentId(nearestOpponent.get().getId());
				} else {
					return new FleeFromCombatAction(parentEntity);
				}
			}
		}

		if (previousAction instanceof MoveInRangeOfTargetCombatAction) {
			combatStateComponent.setHeldLocation(toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition()));
		}

		if (seriouslyInjured() || needsSeriouslyLow()) {
			return new FleeFromCombatAction(parentEntity);
		}

		return attackOrDefendAgainstOpponentAction();
	}

	public void attackedInCombat(Entity attackerEntity) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		combatStateComponent.getOpponentEntityIds().add(attackerEntity.getId());

		if (currentAction instanceof AttackCreatureCombatAction attackAction) {
			attackAction.interrupted(messageDispatcher);
			CombatAction previousAction = currentAction;
			currentAction = new DefensiveCombatAction(parentEntity);
			messageDispatcher.dispatchMessage(MessageType.COMBAT_ACTION_CHANGED, new CombatActionChangedMessage(
					parentEntity, previousAction, currentAction
			));
		}

		changeOpponentIfCanFaceMoreOpponentsAtOnce(combatStateComponent);
	}

	private void changeOpponentIfCanFaceMoreOpponentsAtOnce(CombatStateComponent combatStateComponent) {
		List<Entity> opponentsInMelee = getOpponentsInMelee(parentEntity, gameContext);

		combatStateComponent.setEngagedInMelee(!opponentsInMelee.isEmpty());

		int mostOpponentsInView = 0;
		Long opponentKeepingMostOpponentsInView = null;
		for (Entity cursorOpponent : opponentsInMelee) {
			EntityAssetOrientation orientationToOpponent = EntityAssetOrientation.fromFacingTo8Directions(parentEntity.getLocationComponent().getWorldOrParentPosition().cpy()
					.sub(cursorOpponent.getLocationComponent().getWorldOrParentPosition()));

			int opponentsInView = 0;
			for (Entity otherOpponent : opponentsInMelee) {
				EntityAssetOrientation orientationToOtherOpponent = EntityAssetOrientation.fromFacingTo8Directions(parentEntity.getLocationComponent().getWorldOrParentPosition().cpy()
						.sub(otherOpponent.getLocationComponent().getWorldOrParentPosition()));
				if (!getOrientationsOppositeTo(orientationToOpponent).contains(orientationToOtherOpponent)) {
					opponentsInView++;
				}
			}

			if (opponentsInView > mostOpponentsInView) {
				mostOpponentsInView = opponentsInView;
				opponentKeepingMostOpponentsInView = cursorOpponent.getId();
			}
		}
		if (opponentKeepingMostOpponentsInView != null && !opponentKeepingMostOpponentsInView.equals(combatStateComponent.getTargetedOpponentId())) {
			combatStateComponent.setTargetedOpponentId(opponentKeepingMostOpponentsInView);
		}

	}

	public static List<Entity> getOpponentsInMelee(Entity entityInCombat, GameContext gameContext) {
		CombatStateComponent combatStateComponent = entityInCombat.getComponent(CombatStateComponent.class);
		GridPoint2 parentTilePosition = toGridPoint(entityInCombat.getLocationComponent().getWorldOrParentPosition());
		List<Entity> opponentsInMelee = new ArrayList<>();
		for (Long opponentEntityId : combatStateComponent.getOpponentEntityIds()) {
			Entity opponentEntity = gameContext.getEntities().get(opponentEntityId);
			if (opponentEntity != null) {
				GridPoint2 opponentTilePosition = toGridPoint(opponentEntity.getLocationComponent().getWorldOrParentPosition());
				if (Math.abs(parentTilePosition.x - opponentTilePosition.x) <= 1 &&
						Math.abs(parentTilePosition.y - opponentTilePosition.y) <= 1) {
					opponentsInMelee.add(opponentEntity);
				}
			}
		}
		return opponentsInMelee;
	}

	private AggressionResponse getAggressionResponse() {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		AggressionResponse aggressionResponse = attributes.getRace().getBehaviour().getAggressionResponse();
		if (aggressionResponse == null || aggressionResponse.equals(AggressionResponse.MIXED)) {
			if (new RandomXS128(attributes.getSeed()).nextBoolean()) {
				aggressionResponse = ATTACK;
			} else {
				aggressionResponse = AggressionResponse.FLEE;
			}
		}
		return aggressionResponse;
	}

	private CombatAction attackOrDefendAgainstOpponentAction() {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent.isHasInitiative()) {
			if (isInRangeOfOpponent(parentEntity, gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId()))) {
				return new AttackCreatureCombatAction(parentEntity);
			} else {
				return new MoveInRangeOfTargetCombatAction(parentEntity);
			}
		} else {
			return new DefensiveCombatAction(parentEntity);
		}
	}

	private boolean needsSeriouslyLow() {
		NeedsComponent needsComponent = parentEntity.getComponent(NeedsComponent.class);
		if (needsComponent != null) {
			for (Map.Entry<EntityNeed, Double> needEntry : needsComponent.getAll()) {
				if (needEntry.getValue() <= SERIOUSLY_LOW_NEED_VALUE) {
					return true;
				}
			}

		}
		return false;
	}

	private boolean seriouslyInjured() {
		StatusComponent statusComponent = parentEntity.getComponent(StatusComponent.class);
		if (statusComponent != null && statusComponent.hasSeriousStatusAilment()) {
			return true;
		}

		if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes) {
			Body body = creatureEntityAttributes.getBody();
			for (Map.Entry<BodyPart, BodyPartDamage> damageEntry : body.getAllDamage()) {
				if (damageEntry.getValue().getDamageLevel().equals(BodyPartDamageLevel.BrokenBones)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isInRangeOfOpponent(Entity parentEntity, Entity targetedOpponent) {
		if (targetedOpponent == null) {
			return false;
		}

		CreatureCombat combatStats = new CreatureCombat(parentEntity);
		float range = (float) combatStats.getWeaponRangeAsInt();
		float distanceToOpponent = parentEntity.getLocationComponent().getWorldOrParentPosition().dst(targetedOpponent.getLocationComponent().getWorldOrParentPosition());
		float tileSeparationToOpponent = getTileDistanceBetween(parentEntity.getLocationComponent().getWorldOrParentPosition(), targetedOpponent.getLocationComponent().getWorldOrParentPosition());

		if (tileSeparationToOpponent <= 1) {
			return true;
		} else if (tileSeparationToOpponent <= 2  && range >= 2) {
			return true;
		} else {
			return distanceToOpponent < range;
		}
	}

	public void onEnteringCombat() {
		// Might want to move the implementation of the below to here if it ends up no longer used by normal GOAP
		new EquipWeaponAction(new AssignedGoal(NULL_GOAL, parentEntity, messageDispatcher))
				.update(0.1f, gameContext);
	}

	public void onExitingCombat() {
		parentEntity.getComponent(CombatStateComponent.class).clearState();
		new UnequipWeaponAction(new AssignedGoal(NULL_GOAL, parentEntity, messageDispatcher))
				.update(0.1f, gameContext);

		if (defensePoolEffect != null) {
			defensePoolEffect.getWrappedInstance().allowCompletion();
			defensePoolEffect = null;
		}
	}

	public CombatAction getCurrentAction() {
		return currentAction;
	}

	private static float getTileDistanceBetween(Vector2 a, Vector2 b) {
		GridPoint2 gridPointA = toGridPoint(a);
		GridPoint2 gridPointB = toGridPoint(b);
		return Math.max(Math.abs(gridPointA.x - gridPointB.x), Math.abs(gridPointA.y - gridPointB.y));
	}

	public boolean isActionComplete() {
		return currentAction == null || currentAction.isCompleted();
	}

	public void makeAttackOfOpportunity(Entity targetEntity) {
		CreatureCombat combat = new CreatureCombat(parentEntity);
		if (combat.getWeaponRangeAsInt() <= 2) {
			// Only make "melee" attacks of opportunity
			attackOfOpportunityAction = new AttackCreatureCombatAction(parentEntity);
			attackOfOpportunityAction.setOverrideTarget(targetEntity.getId());
			parentEntity.getComponent(CombatStateComponent.class).setAttackOfOpportunityMadeThisRound(true);
		}
	}

	public void interrupted() {
		if (currentAction != null) {
			currentAction.interrupted(messageDispatcher);
			messageDispatcher.dispatchMessage(MessageType.COMBAT_ACTION_CHANGED, new CombatActionChangedMessage(parentEntity, currentAction, null));
			currentAction = null;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (currentAction != null) {
			JSONObject actionJson = new JSONObject(true);
			actionJson.put("_class", currentAction.getClass().getName());
			currentAction.writeTo(actionJson, savedGameStateHolder);
			asJson.put("currentAction", actionJson);
		}

		if (attackOfOpportunityAction != null) {
			JSONObject actionJson = new JSONObject(true);
			attackOfOpportunityAction.writeTo(actionJson, savedGameStateHolder);
			asJson.put("attackOfOpportunityAction", actionJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject actionJson = asJson.getJSONObject("currentAction");
		if (actionJson != null) {
			String className = actionJson.getString("_class");
			this.currentAction = CombatAction.newInstance(ReflectionUtils.forName(className), Entity.NULL_ENTITY);
			this.currentAction.readFrom(asJson, savedGameStateHolder, relatedStores);
		}

		actionJson = asJson.getJSONObject("attackOfOpportunityAction");
		if (actionJson != null) {
			String className = actionJson.getString("_class");
			this.attackOfOpportunityAction = new AttackCreatureCombatAction(Entity.NULL_ENTITY);
			this.attackOfOpportunityAction.readFrom(asJson, savedGameStateHolder, relatedStores);
		}
	}

	@Override
	public void typeFound(ParticleEffectType type) {
		this.defensePoolEffectType = type;
	}

	@Override
	public void particleCreated(ParticleEffectInstance instance) {
		this.defensePoolEffect = instance;
	}
}
