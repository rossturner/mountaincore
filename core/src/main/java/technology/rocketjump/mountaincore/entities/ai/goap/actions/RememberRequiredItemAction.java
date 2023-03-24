package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.components.creature.MemoryComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static technology.rocketjump.mountaincore.entities.ai.memory.MemoryType.LACKING_REQUIRED_ITEM;

public class RememberRequiredItemAction extends Action {
	public RememberRequiredItemAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		MemoryComponent memoryComponent = parent.parentEntity.getComponent(MemoryComponent.class);
		Memory relevantMemory = null;
		List<Memory> shortTermMemories = new ArrayList<>(memoryComponent.getShortTermMemories(gameContext.getGameClock()));
		Collections.shuffle(shortTermMemories, gameContext.getRandom());
		for (Memory memory : shortTermMemories) {
			if (memory.getType().equals(LACKING_REQUIRED_ITEM)) {
				relevantMemory = memory;
				break;
			}
		}

		if (relevantMemory == null || (relevantMemory.getRelatedItemType() == null && relevantMemory.getRelatedAmmoType() == null)) {
			completionType = CompletionType.FAILURE;
		} else {
			parent.setRelevantMemory(relevantMemory);
			memoryComponent.removeByType(relevantMemory.getType()); // Need to remove memory so we're not always fixated on the oldest memory
			completionType = CompletionType.SUCCESS;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}
}
