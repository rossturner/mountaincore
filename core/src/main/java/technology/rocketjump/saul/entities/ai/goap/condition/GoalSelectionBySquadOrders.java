package technology.rocketjump.saul.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;

public class GoalSelectionBySquadOrders implements GoalSelectionCondition {

	public final SquadOrderType orderType;

	@JsonCreator
	public GoalSelectionBySquadOrders(
			@JsonProperty("orderType") SquadOrderType orderType
	) {
		this.orderType = orderType;
	}

	@JsonIgnore
	@Override
	public boolean apply(Entity parentEntity, GameContext gameContext) {
		MilitaryComponent militaryComponent = parentEntity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.getSquadId() != null) {
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());
			if (squad != null) {
				return squad.getCurrentOrderType().equals(orderType);
			}
		}
		return false;
	}
}
