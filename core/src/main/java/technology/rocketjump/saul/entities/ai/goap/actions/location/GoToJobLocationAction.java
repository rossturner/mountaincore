package technology.rocketjump.saul.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.gamecontext.GameContext;

public class GoToJobLocationAction extends GoToLocationAction {
	public GoToJobLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getParentGoal() != null) {
			return new GoToJobLocationAction(parent.getParentGoal()).selectDestination(gameContext);
		} else if (parent.getAssignedJob() == null) {
			return null;
		} else {
			return getJobLocation(gameContext);
		}
	}

}
