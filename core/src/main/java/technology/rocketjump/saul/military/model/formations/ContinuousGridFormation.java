package technology.rocketjump.saul.military.model.formations;

import technology.rocketjump.saul.mapping.tile.CompassDirection;

import java.util.List;

public class ContinuousGridFormation extends AbstractGridFormation {

	@Override
	public String getFormationName() {
		return "CONTINUOUS_GRID";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.values());
	}

	@Override
	protected int getSpacing() {
		return 1;
	}

}
