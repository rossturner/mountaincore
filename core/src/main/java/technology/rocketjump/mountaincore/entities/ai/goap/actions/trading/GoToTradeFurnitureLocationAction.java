package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class GoToTradeFurnitureLocationAction extends GoToLocationAction {
	public GoToTradeFurnitureLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getPlannedTrade() != null) {
			return parent.getPlannedTrade().getImportExportFurniture().getLocationComponent().getWorldOrParentPosition();
		} else {
			return null;
		}
	}
}
