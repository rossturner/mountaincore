package technology.rocketjump.saul.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.ai.memory.MemoryType;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.entities.components.creature.NeedsComponent;
import technology.rocketjump.saul.environment.GameClock;

import java.util.Collection;

public class GoalSelectionByMemory implements GoalSelectionCondition {

	public final MemoryType memoryType;

	@JsonCreator
	public GoalSelectionByMemory(@JsonProperty("memoryType") MemoryType memoryType) {
		this.memoryType = memoryType;
	}

	@JsonIgnore
	@Override
	public boolean apply(GameClock gameClock, NeedsComponent needsComponent, MemoryComponent memoryComponent) {
		if (memoryComponent == null) {
			return false;
		}

		Collection<Memory> shortTermMemories = memoryComponent.getShortTermMemories(gameClock);
		for (Memory shortTermMemory : shortTermMemories) {
			if (shortTermMemory.getType().equals(memoryType)) {
				return true;
			}
		}
		return false;
	}
}
