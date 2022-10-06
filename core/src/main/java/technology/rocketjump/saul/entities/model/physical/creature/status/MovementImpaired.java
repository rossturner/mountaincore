package technology.rocketjump.saul.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartFunction;
import technology.rocketjump.saul.gamecontext.GameContext;

public class MovementImpaired extends StatusEffect {

	public MovementImpaired() {
		super(null, null, null, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		SteeringComponent steeringComponent = parentEntity.getBehaviourComponent().getSteeringComponent();
		boolean hasOtherMeansOfMovement = false;
		if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureAttributes) {
			for (BodyPart workingBodyPart : creatureAttributes.getBody().getAllWorkingBodyParts()) {
				if (workingBodyPart.getPartDefinition().getFunction() == BodyPartFunction.MOVEMENT) {
					hasOtherMeansOfMovement = true;
				}
			}
		}
		if (!hasOtherMeansOfMovement) {
			steeringComponent.setImmobilised(true);
		}
		steeringComponent.setMovementImpaired(true);
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		return false;
	}

	@Override
	public String getI18Key() {
		return "STATUS.MOVEMENT_IMPAIRED";
	}

}
