package technology.rocketjump.mountaincore.entities.ai.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.Goal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToCombatOpponentAction;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CombatBehaviour;
import technology.rocketjump.mountaincore.entities.components.creature.CombatStateComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class MoveInRangeOfTargetCombatAction extends CombatAction {

	private GoToCombatOpponentAction goToLocationAction;

	public MoveInRangeOfTargetCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) throws ExitingCombatException {
		if (goToLocationAction == null) {
			goToLocationAction = new GoToCombatOpponentAction(new AssignedGoal(Goal.NULL_GOAL, parentEntity, messageDispatcher));
			parentEntity.getComponent(CombatStateComponent.class).setHeldLocation(null);
		}

		goToLocationAction.update(deltaTime, gameContext);

		Action.CompletionType completion = isCompleted(goToLocationAction, gameContext);
		if (completion != null) {
			if (completion.equals(Action.CompletionType.FAILURE)) {
				throw new ExitingCombatException();
			}
			CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
			if (CombatBehaviour.isInRangeOfOpponent(parentEntity, lookupOpponent(combatStateComponent, gameContext))) {
				parentEntity.getBehaviourComponent().getSteeringComponent().destinationReached();
				this.completed = true;
			} else {
				// Opponent has moved away
				this.goToLocationAction = null;
			}
		} else {
			CreatureCombat combatStats = new CreatureCombat(parentEntity);
			if (combatStats.getEquippedWeapon().isRanged()) {
				Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
				CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
				Entity opponentEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
				if (opponentEntity != null) {
					Vector2 opponentPosition = opponentEntity.getLocationComponent().getWorldOrParentPosition();
					float distanceToOpponent = parentPosition.dst(opponentPosition);
					if (distanceToOpponent < ((float)combatStats.getEquippedWeapon().getRange()) * 0.8f) {
						// Checking opponent is within 80% of range so that they don't just immediately move out of range again
						parentEntity.getBehaviourComponent().getSteeringComponent().destinationReached();
						this.completed = true;
					}
				}
			}
		}
	}

	private Entity lookupOpponent(CombatStateComponent combatStateComponent, GameContext gameContext) {
		if (combatStateComponent.getTargetedOpponentId() == null) {
			return null;
		} else {
			return gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
		}
	}

	public static Action.CompletionType isCompleted(GoToLocationAction action, GameContext gameContext) {
		try {
			return action.isCompleted(gameContext);
		} catch (SwitchGoalException e) {
			Logger.error("Need to handle SwitchGoalException in " + MoveInRangeOfTargetCombatAction.class.getSimpleName());
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

	// don't do persistence, just create a new GoToLocationAction

}
