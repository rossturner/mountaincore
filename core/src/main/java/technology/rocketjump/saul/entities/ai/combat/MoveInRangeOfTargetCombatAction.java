package technology.rocketjump.saul.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.Goal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.ai.goap.actions.location.GoToCombatOpponentAction;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class MoveInRangeOfTargetCombatAction extends CombatAction {

	private GoToCombatOpponentAction goToLocationAction;

	public MoveInRangeOfTargetCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) throws ExitingCombatException {
		if (goToLocationAction == null) {
			goToLocationAction = new GoToCombatOpponentAction(new AssignedGoal(Goal.NULL_GOAL, parentEntity, messageDispatcher));
		}

		goToLocationAction.update(deltaTime, gameContext);

		Action.CompletionType completion = isCompleted(goToLocationAction, gameContext);
		if (completion != null) {
			if (completion.equals(Action.CompletionType.FAILURE)) {
				throw new ExitingCombatException();
			}
			this.completed = true;
		} else {
			CreatureCombat combatStats = new CreatureCombat(parentEntity);
			if (combatStats.getWeaponRangeAsInt() > 1) {
				Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
				CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
				Entity opponentEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
				if (opponentEntity != null) {
					Vector2 opponentPosition = opponentEntity.getLocationComponent().getWorldOrParentPosition();
					float distanceToOpponent = parentPosition.dst(opponentPosition);
					if (distanceToOpponent < combatStats.getEquippedWeapon().getRange() * 0.8f) {
						// Checking opponent is within 80% of range so that they don't just immediately move out of range again
						this.completed = true;
					}
				}
			}
		}
	}

	private Action.CompletionType isCompleted(GoToCombatOpponentAction action, GameContext gameContext) {
		try {
			return action.isCompleted(gameContext);
		} catch (SwitchGoalException e) {
			Logger.error("Need to handle SwitchGoalException in " + getClass().getSimpleName());
			return Action.CompletionType.FAILURE;
		}
	}

	@Override
	public void interrupted(MessageDispatcher messageDispatcher) {

	}

	@Override
	public boolean completesInOneRound() {
		return false;
	}

	@Override
	public void onRoundCompletion() {
		super.onRoundCompletion();

		this.goToLocationAction = null; // Null out every round so that pathfinding is reset in case the opponent keeps changing position
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		throw new NotImplementedException("Need to implement writeTo() in " + getClass().getSimpleName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
