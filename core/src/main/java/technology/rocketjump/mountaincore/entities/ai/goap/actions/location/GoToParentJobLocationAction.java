package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoToParentJobLocationAction extends GoToLocationAction {
	public GoToParentJobLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getParentGoal() != null) {
			return new GoToJobLocationAction(parent.getParentGoal()).selectDestination(gameContext);
		} else {
			return null;
		}
	}

}
