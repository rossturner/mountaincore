package technology.rocketjump.mountaincore.military.model.formations;

import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;

import java.util.List;

public class VerticalContinuousLineFormation extends AbstractSpacedFormation {

	@Override
	public String getFormationName() {
		return "VERTICAL_CONTINUOUS_LINE";
	}

	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.COLUMN";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_column";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.NORTH, CompassDirection.SOUTH);
	}

	@Override
	protected int getSpacing() {
		return 1;
	}

}
