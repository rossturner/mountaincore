package technology.rocketjump.saul.military.model.formations;

import technology.rocketjump.saul.mapping.tile.CompassDirection;

import java.util.List;

public class SingleSpacedGridFormation extends AbstractGridFormation {

	@Override
	public String getFormationName() {
		return "SINGLE_SPACED_GRID";
	}

	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.GRID_SINGLE";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_grid_single";
	}

	@Override
	protected List<CompassDirection> getFormationDirections() {
		return List.of(CompassDirection.values());
	}

	@Override
	protected int getSpacing() {
		return 2;
	}

}
