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
import technology.rocketjump.saul.entities.ai.goap.actions.EquipWeaponAction;
import technology.rocketjump.saul.entities.ai.goap.actions.UnequipWeaponAction;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.AggressionResponse;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.CombatActionChangedMessage;
import technology.rocketjump.saul.messaging.types.ParticleEffectTypeCallback;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.particles.custom_libgdx.DefensePoolBarEffect;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.particles.model.ParticleEffectType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.saul.combat.CombatMessageHandler.getOrientationsOppositeTo;
import static technology.rocketjump.saul.entities.ai.goap.Goal.NULL_GOAL;
import static technology.rocketjump.saul.entities.model.physical.creature.AggressionResponse.ATTACK;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

// Although this is coded as a Component, it is always encapsulated in CreatureBehaviour
public class CombatBehaviour implements ParentDependentEntityComponent, ParticleEffectTypeCallback, ParticleRequestMessage.ParticleCreationCallback {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private CombatAction currentAction;
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

		messageDispatcher.dispatchMessage(MessageType.GET_DEFENSE_POOL_EFFECT_TYPE, this);
	}

	/**
	 * This is expected to be called by CombatTracker for it to know what is going on in the round
	 */
	public CombatAction selectNewActionForRound(boolean stunned) {
		if (stunned) {
			return new StunnedCombatAction(parentEntity);
		}

		CombatAction previousAction = currentAction;
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
		}

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
			}
		}
	}

	public void sufferedCombatDamage(Entity attackerEntity) {
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
		GridPoint2 parentTilePosition = toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition());
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
		if (previousAction instanceof MoveInRangeOfTargetCombatAction) {
			combatStateComponent.setHeldLocation(toGridPoint(parentEntity.getLocationComponent().getWorldOrParentPosition()));
		}
		return attackOrDefendAgainstOpponentAction();
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

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (currentAction != null) {
			JSONObject actionJson = new JSONObject(true);
			actionJson.put("_class", currentAction.getClass().getName());
			currentAction.writeTo(actionJson, savedGameStateHolder);
			asJson.put("currentAction", actionJson);
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
