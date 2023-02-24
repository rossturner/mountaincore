package technology.rocketjump.saul.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class GoalSelectionByDrivingVehicle implements GoalSelectionCondition {

	@JsonCreator
	public GoalSelectionByDrivingVehicle() {
	}

	@JsonIgnore
	@Override
	public boolean apply(Entity parentEntity, GameContext gameContext) {
		return parentEntity.isDrivingVehicle();
	}
}
