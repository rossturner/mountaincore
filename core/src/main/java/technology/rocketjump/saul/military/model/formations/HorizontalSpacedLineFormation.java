package technology.rocketjump.saul.military.model.formations;

import technology.rocketjump.saul.mapping.tile.CompassDirection;

import java.util.List;

public class HorizontalSpacedLineFormation extends AbstractSpacedFormation {

	@Override
	public String getFormationName() {
		return "HORIZONTAL_SPACED_LINE";
	}

	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.LINE_SINGLE";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_line_single";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.EAST, CompassDirection.WEST);
	}

	@Override
	protected int getSpacing() {
		return 2;
	}

}
