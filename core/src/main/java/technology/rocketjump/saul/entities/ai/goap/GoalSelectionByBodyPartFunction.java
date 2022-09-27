package technology.rocketjump.saul.entities.ai.goap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartFunction;
import technology.rocketjump.saul.gamecontext.GameContext;

public class GoalSelectionByBodyPartFunction implements GoalSelectionCondition {
	public BodyPartFunction need;

	@JsonCreator
	public GoalSelectionByBodyPartFunction(@JsonProperty("need") BodyPartFunction need) {
		this.need = need;
	}

	@JsonIgnore
	@Override
	public boolean apply(Entity parentEntity, GameContext gameContext) {
		if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureAttributes) {
			for (BodyPart workingBodyPart : creatureAttributes.getBody().getAllWorkingBodyParts()) {
				if (need == workingBodyPart.getPartDefinition().getFunction()) {
					return true;
				}
			}
		}
		return false;
	}
}
