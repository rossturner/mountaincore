package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.saul.gamecontext.GameContext;

public class GoToWagonWithItemAction extends GoToLocationAction {
	public GoToWagonWithItemAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getPlannedTrade() != null) {
			return gameContext.getEntity(parent.getPlannedTrade().getHaulingAllocation().getSourceContainerId()).getLocationComponent().getWorldOrParentPosition();
		} else {
			return null;
		}
	}
}
