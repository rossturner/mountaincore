package technology.rocketjump.saul.military.model.formations;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSpacedFormation implements SquadFormation {

	protected abstract List<CompassDirection> getFormationDirections();

	protected abstract int getSpacing();

	@Override
	public GridPoint2 getFormationPosition(int squadMemberIndex, GridPoint2 centralLocation, GameContext gameContext) {
		MapTile centralTile = gameContext.getAreaMap().getTile(centralLocation);
		if (centralTile == null) {
			return null;
		}

		if (squadMemberIndex == 0 && centralTile.isNavigable(null)) {
			return centralLocation;
		}

		List<GridPoint2> availablePositions = getAvailablePositions(centralTile, gameContext, squadMemberIndex + 1);
		if (availablePositions.size() > squadMemberIndex) {
			return availablePositions.get(squadMemberIndex);
		} else {
			return null;
		}
	}


	private List<GridPoint2> getAvailablePositions(MapTile centralTile, GameContext gameContext, int positionsRequired) {
		List<GridPoint2> availablePositions = new ArrayList<>();
		for (int cursor = 0; cursor < 100; cursor++) {
			List<CompassDirection> formationDirections = getFormationDirections();
			CompassDirection direction = formationDirections.get(cursor % formationDirections.size());

			int distanceOffset = ((cursor / formationDirections.size()) + 1) * getSpacing();
			MapTile nextTile = gameContext.getAreaMap().getTile(centralTile.getTileX() + (direction.getXOffset() * distanceOffset),
					centralTile.getTileY() + (direction.getYOffset() + distanceOffset));

			if (nextTile != null && nextTile.getRegionId() == centralTile.getRegionId() && nextTile.isNavigable(null)) {
				availablePositions.add(nextTile.getTilePosition());
			}

			if (availablePositions.size() >= positionsRequired) {
				return availablePositions;
			}
		}
		return availablePositions;
	}

	@Override
	public String toString() {
		return getFormationName();
	}
}
