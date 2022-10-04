package technology.rocketjump.saul.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.InvasionCreatureGroup;
import technology.rocketjump.saul.entities.behaviour.creature.InvasionStage;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class GoalSelectionByInvasionStage implements GoalSelectionCondition {

	public final InvasionStage invasionStage;

	@JsonCreator
	public GoalSelectionByInvasionStage(
			@JsonProperty("invasionStage") InvasionStage invasionStage
	) {
		this.invasionStage = invasionStage;
	}

	@JsonIgnore
	@Override
	public boolean apply(Entity parentEntity, GameContext gameContext) {
		if (parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
				creatureBehaviour.getCreatureGroup() != null && creatureBehaviour.getCreatureGroup() instanceof InvasionCreatureGroup invasionCreatureGroup) {
			return invasionCreatureGroup.getInvasionStage().equals(invasionStage);
		}
		return false;
	}
}
