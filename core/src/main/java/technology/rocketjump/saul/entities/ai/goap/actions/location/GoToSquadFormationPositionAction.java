package technology.rocketjump.saul.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.military.model.Squad;

import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class GoToSquadFormationPositionAction extends GoToLocationAction {

	public GoToSquadFormationPositionAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		MilitaryComponent militaryComponent = parent.parentEntity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null) {
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());
			if (squad != null && squad.getGuardingLocation() != null) {
				return toVector(squad.getFormation().getFormationPosition(squad.getMemberIndex(parent.parentEntity.getId()),
						squad.getGuardingLocation(),
						gameContext, squad.getMemberEntityIds().size()));
			}
		}
		return null;
	}

}
