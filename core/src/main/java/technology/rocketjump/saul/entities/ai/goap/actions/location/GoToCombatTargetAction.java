package technology.rocketjump.saul.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class GoToCombatTargetAction extends GoToLocationAction {
	public GoToCombatTargetAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getAssignedJob() == null || parent.getAssignedJob().getTargetId() == null) {
			Logger.error("Was expecting a target for " + this.getSimpleName());
			return null;
		}

		Entity targetEntity = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (targetEntity != null) {
			return targetEntity.getLocationComponent(true).getWorldOrParentPosition();
		} else {
			return null;
		}
	}

}
