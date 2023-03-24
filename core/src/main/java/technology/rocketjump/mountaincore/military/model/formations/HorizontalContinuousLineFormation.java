package technology.rocketjump.mountaincore.military.model.formations;

import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;

import java.util.List;

public class HorizontalContinuousLineFormation extends AbstractSpacedFormation {

	@Override
	public String getFormationName() {
		return "HORIZONTAL_CONTINUOUS_LINE";
	}

	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.LINE";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_line";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.EAST, CompassDirection.WEST);
	}

	@Override
	protected int getSpacing() {
		return 1;
	}

}
