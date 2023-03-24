package technology.rocketjump.mountaincore.military.model.formations;

import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;

import java.util.List;

public class VerticalDoubleSpacedLineFormation extends AbstractSpacedFormation {

	@Override
	public String getFormationName() {
		return "VERTICAL_DOUBLE_SPACED_LINE";
	}

	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.COLUMN_DOUBLE";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_column_double";
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
