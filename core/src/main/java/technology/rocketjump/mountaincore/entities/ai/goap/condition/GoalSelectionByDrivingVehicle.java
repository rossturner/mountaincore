package technology.rocketjump.mountaincore.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoalSelectionByDrivingVehicle implements GoalSelectionCondition {

	private final boolean value;

	@JsonCreator
	public GoalSelectionByDrivingVehicle(@JsonProperty("value") boolean value) {
		this.value = value;
	}

	@JsonIgnore
	@Override
	public boolean apply(Entity parentEntity, GameContext gameContext) {
		return parentEntity.isDrivingVehicle() == value;
	}
}
