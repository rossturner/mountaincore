package technology.rocketjump.mountaincore.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.mountaincore.entities.behaviour.creature.TraderGroupStage;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoalSelectionByTradersStage implements GoalSelectionCondition {

	public final TraderGroupStage stage;

	@JsonCreator
	public GoalSelectionByTradersStage(
			@JsonProperty("stage") TraderGroupStage stage
	) {
		this.stage = stage;
	}

	@JsonIgnore
	@Override
	public boolean apply(Entity parentEntity, GameContext gameContext) {
		if (parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
				creatureBehaviour.getCreatureGroup() != null && creatureBehaviour.getCreatureGroup() instanceof TraderCreatureGroup traderCreatureGroup) {
			return traderCreatureGroup.getStage().equals(stage);
		}
		return false;
	}
}
