package technology.rocketjump.saul.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.saul.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.saul.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.saul.environment.GameClock;

public class GoalSelectionByNeed implements GoalSelectionCondition {

	public final EntityNeed need;
	public final Operator operator;
	public final Double value;

	@JsonCreator
	public GoalSelectionByNeed(
			@JsonProperty("need") EntityNeed need,
			@JsonProperty("operator") Operator operator,
			@JsonProperty("value") Double value) {
		this.need = need;
		this.operator = operator;
		this.value = value;
	}

	@JsonIgnore
	@Override
	public boolean apply(GameClock gameClock, NeedsComponent needsComponent, MemoryComponent memoryComponent) {
		if (needsComponent == null || !needsComponent.has(need)) {
			return false;
		}
		double needValue = needsComponent.getValue(need);
		return operator.apply(needValue, value);
	}
}
