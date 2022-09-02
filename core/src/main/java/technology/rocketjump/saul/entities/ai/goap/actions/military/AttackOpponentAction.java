package technology.rocketjump.saul.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.ai.memory.MemoryType;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class AttackOpponentAction extends Action {

	public AttackOpponentAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		MemoryComponent memoryComponent = parent.parentEntity.getOrCreateComponent(MemoryComponent.class);
		Memory memory = new Memory(MemoryType.ABOUT_TO_ATTACK_CREATURE, gameContext.getGameClock());
		CombatStateComponent combatStateComponent = parent.parentEntity.getComponent(CombatStateComponent.class);
		memory.setRelatedEntityId(combatStateComponent.getTargetedOpponentId());
		memory.setRelatedEntityIds(combatStateComponent.getOpponentEntityIds());
		memoryComponent.addShortTerm(memory, gameContext.getGameClock());
		completionType = CompletionType.SUCCESS;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
