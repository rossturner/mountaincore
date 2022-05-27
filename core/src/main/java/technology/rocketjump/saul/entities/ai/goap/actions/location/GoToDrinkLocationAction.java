package technology.rocketjump.saul.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.gamecontext.GameContext;

import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class GoToDrinkLocationAction extends GoToLocationAction {
	public GoToDrinkLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getLiquidAllocation() == null || parent.getLiquidAllocation().getTargetZoneTile().getAccessLocation() == null) {
			return null;
		}
		return toVector(parent.getLiquidAllocation().getTargetZoneTile().getAccessLocation());
	}

}
