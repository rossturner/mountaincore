package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.ai.combat.CombatAction;
import technology.rocketjump.saul.entities.ai.combat.ExitingCombatException;
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

import static technology.rocketjump.saul.entities.model.physical.creature.AggressionResponse.ATTACK;

// Although this is coded as a Component, it is always encapsulated in CreatureBehaviour
public class CombatBehaviour implements ParentDependentEntityComponent {

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

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
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		CombatAction previousAction = combatStateComponent.getCurrentAction();
		CombatAction newAction;
		if (previousAction == null) {
			newAction = initialCombatAction();
		} else {
			// TODO determine what to do next
			newAction = null;
		}
		combatStateComponent.setCurrentAction(newAction);
		return newAction;
	}

	public CombatAction changeCombatAction() {
		// TODO something to decide to switch to the defensive if attacked by multiple opponents in melee

		// otherwise return null
		return null;
	}

	public void combatRoundStarted() {
	}

	public void update(float deltaTime) throws ExitingCombatException {

	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not yet implemented " + this.getClass().getSimpleName() + ".clone()");
	}

	private CombatAction initialCombatAction() {
		if (getAggressionResponse().equals(ATTACK)) {
			return attackOrDefendAgainstOpponentAction();
		} else {
			return fleeFromOpponentAction();
		}
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

		return null;
	}

	private CombatAction fleeFromOpponentAction() {
		return null;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		//TODO persist and retrieve state
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
