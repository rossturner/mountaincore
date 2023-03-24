package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.misc.VectorUtils;

public class GoToDrinkLocationAction extends GoToLocationAction {
	public GoToDrinkLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getLiquidAllocation() == null || parent.getLiquidAllocation().getTargetZoneTile().getAccessLocation() == null) {
			return null;
		}
		return VectorUtils.toVector(parent.getLiquidAllocation().getTargetZoneTile().getAccessLocation());
	}

}
