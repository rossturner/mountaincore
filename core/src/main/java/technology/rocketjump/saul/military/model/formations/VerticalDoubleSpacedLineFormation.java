package technology.rocketjump.saul.military.model.formations;

import technology.rocketjump.saul.mapping.tile.CompassDirection;

import java.util.List;

public class VerticalDoubleSpacedLineFormation extends AbstractSpacedFormation {

	@Override
	public String getFormationName() {
		return "VERTICAL_DOUBLE_SPACED_LINE";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.NORTH, CompassDirection.SOUTH);
	}

	@Override
	protected int getSpacing() {
		return 3;
	}

}
