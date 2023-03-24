package technology.rocketjump.mountaincore.military.model.formations;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSpacedFormation implements SquadFormation {

	protected abstract List<CompassDirection> getFormationDirections();

	protected abstract int getSpacing();

	@Override
	public GridPoint2 getFormationPosition(int squadMemberIndex, GridPoint2 centralLocation, GameContext gameContext, int totalSquadMembers) {
		MapTile centralTile = gameContext.getAreaMap().getTile(centralLocation);
		if (centralTile == null || !centralTile.isNavigable(null)) {
			return null;
		}

		if (squadMemberIndex == 0 && centralTile.isNavigable(null)) {
			return centralLocation;
		}

		List<GridPoint2> availablePositions = getAvailablePositions(centralTile, gameContext, squadMemberIndex + 1);
		if (availablePositions.size() > squadMemberIndex) {
			return availablePositions.get(squadMemberIndex - 1);
		} else {
			return null;
		}
	}


	private List<GridPoint2> getAvailablePositions(MapTile centralTile, GameContext gameContext, int positionsRequired) {
		List<GridPoint2> availablePositions = new ArrayList<>();
		int distanceOffset = getSpacing();
		int attempts = 0; // used to ensure we don't create an infinite loop
		while (attempts < positionsRequired + 200) {
			for (CompassDirection direction : getFormationDirections()) {
				MapTile nextTile = gameContext.getAreaMap().getTile(centralTile.getTileX() + (direction.getXOffset() * distanceOffset),
						centralTile.getTileY() + (direction.getYOffset() * distanceOffset));

				if (nextTile != null && nextTile.getRegionId() == centralTile.getRegionId() && nextTile.isNavigable(null)) {
					availablePositions.add(nextTile.getTilePosition());
				}

				attempts++;
				if (availablePositions.size() >= positionsRequired) {
					return availablePositions;
				}
			}

			distanceOffset += getSpacing();
		}
		return availablePositions;
	}

	@Override
	public String toString() {
		return getFormationName();
	}
}
