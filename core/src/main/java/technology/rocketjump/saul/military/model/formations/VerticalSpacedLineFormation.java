package technology.rocketjump.saul.military.model.formations;

import technology.rocketjump.saul.mapping.tile.CompassDirection;

import java.util.List;

public class VerticalSpacedLineFormation extends AbstractSpacedFormation {

	@Override
	public String getFormationName() {
		return "VERTICAL_SPACED_LINE";
	}

	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.COLUMN_SINGLE";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_column_single";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.NORTH, CompassDirection.SOUTH);
	}

	@Override
	protected int getSpacing() {
		return 2;
	}

}
