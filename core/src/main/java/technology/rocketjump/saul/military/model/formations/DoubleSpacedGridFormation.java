package technology.rocketjump.saul.military.model.formations;

import technology.rocketjump.saul.mapping.tile.CompassDirection;

import java.util.List;

public class DoubleSpacedGridFormation extends AbstractSpacedFormation {

	@Override
	public String getFormationName() {
		return "DOUBLE_SPACED_GRID";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.values());
	}

	@Override
	protected int getSpacing() {
		return 3;
	}

}
