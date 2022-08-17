package technology.rocketjump.saul.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.environment.GameClock;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = GoalSelectionByMemory.class, name = "MEMORY"),
		@JsonSubTypes.Type(value = GoalSelectionByNeed.class, name = "NEED"),
		@JsonSubTypes.Type(value = GoalSelectionByItemAssignment.class, name = "ITEM_ASSIGNED"),
})
public interface GoalSelectionCondition {

	@JsonIgnore
	boolean apply(GameClock gameClock, Entity parentEntity);

}
