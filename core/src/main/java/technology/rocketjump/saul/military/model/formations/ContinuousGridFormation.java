package technology.rocketjump.saul.military.model.formations;

import technology.rocketjump.saul.mapping.tile.CompassDirection;

import java.util.List;

public class ContinuousGridFormation extends AbstractGridFormation {

	@Override
	public String getFormationName() {
		return "CONTINUOUS_GRID";
	}

	@Override
	public String getI18nKey() {
		return "MILITARY.FORMATION.GRID";
	}

	@Override
	public String getDrawableIconName() {
		return "icon_formation_grid";
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
