package technology.rocketjump.mountaincore.military.model.formations;

import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;

import java.util.List;

public class DoubleSpacedGridFormation extends AbstractGridFormation {

	@Override
	public String getFormationName() {
		return "DOUBLE_SPACED_GRID";
	}


	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.GRID_DOUBLE";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_grid_double";
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
