package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.misc.VectorUtils;

public class GoToSquadFormationPositionAction extends GoToLocationAction {

	public GoToSquadFormationPositionAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		MilitaryComponent militaryComponent = parent.parentEntity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.getSquadId() != null) {
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());
			if (squad != null && squad.getGuardingLocation() != null) {
				return VectorUtils.toVector(squad.getFormation().getFormationPosition(squad.getMemberIndex(parent.parentEntity.getId()),
						squad.getGuardingLocation(),
						gameContext, squad.getMemberEntityIds().size()));
			}
		}
		return null;
	}

}
