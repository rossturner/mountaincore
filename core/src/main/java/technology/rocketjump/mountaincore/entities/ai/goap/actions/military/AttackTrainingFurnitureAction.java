package technology.rocketjump.mountaincore.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.combat.AttackCreatureCombatAction;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.combat.CombatTracker.COMBAT_ROUND_DURATION;

public class AttackTrainingFurnitureAction extends Action {

	private int attacksToMake = 5;
	/*
	 Note this isn't being persisted though it should be - because we don't have the parent AssignedGoal.parentEntity
	 during persistence logic - the parentEntity gets set in the init() step *after* readFrom() is called
	 so we can't instantiate a child CombatAction here correctly.

	 This means that loading into a saved game with this action in progress might take a few more seconds to complete than it should
	 which does not feel like a terrible problem, despite not being ideal
	 */
	private transient AttackCreatureCombatAction currentAttackCombatAction;

	public AttackTrainingFurnitureAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (attacksToMake <= 0) {
			completionType = CompletionType.SUCCESS;
			return;
		}

		if (currentAttackCombatAction == null) {
			if (!hasRequiredAmmo(deltaTime, gameContext)) {
				completionType = CompletionType.FAILURE;
				return;
			}

			currentAttackCombatAction = new AttackCreatureCombatAction(parent.parentEntity);
			currentAttackCombatAction.setOverrideTarget(parent.getAssignedFurnitureId());
			currentAttackCombatAction.setTimeUntilAttack(COMBAT_ROUND_DURATION);
		}

		currentAttackCombatAction.update(deltaTime, gameContext, parent.messageDispatcher);

		if (currentAttackCombatAction.isCompleted()) {
			currentAttackCombatAction = null;
			attacksToMake--;
		}
	}

	private boolean hasRequiredAmmo(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		CheckAmmoAvailableAction checkAmmoAvailableAction = new CheckAmmoAvailableAction(parent);
		checkAmmoAvailableAction.update(deltaTime, gameContext);
		return CompletionType.SUCCESS.equals(checkAmmoAvailableAction.isCompleted(gameContext));
	}

	@Override
	public void actionInterrupted(GameContext gameContext) {
		super.actionInterrupted(gameContext);

		if (currentAttackCombatAction != null) {
			currentAttackCombatAction.interrupted(parent.messageDispatcher);
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("attacksToMake", attacksToMake);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.attacksToMake = asJson.getIntValue("attacksToMake");
	}

}
