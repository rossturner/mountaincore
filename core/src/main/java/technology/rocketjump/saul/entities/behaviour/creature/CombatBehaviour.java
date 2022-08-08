package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.NotImplementedException;
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
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.Goal.NULL_GOAL;
import static technology.rocketjump.saul.entities.model.physical.creature.AggressionResponse.ATTACK;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

// Although this is coded as a Component, it is always encapsulated in CreatureBehaviour
public class CombatBehaviour implements ParentDependentEntityComponent {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private CombatAction currentAction;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
	}

	/**
	 * This is expected to be called by CombatTracker for it to know what is going on in the round
	 */
	public CombatAction selectNewActionForRound() {
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

	}

	public void attackDamageSuffered() {
		// TODO might want to change targeted opponent if multiple basing me
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
		throw new NotImplementedException("TODO");
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
		throw new NotImplementedException("Must implement writeTo() in " + getClass().getSimpleName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
