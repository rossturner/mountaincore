package technology.rocketjump.saul.military.model.formations;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGridFormation implements SquadFormation {

	protected abstract List<CompassDirection> getFormationDirections();

	protected abstract int getSpacing();

	@Override
	public GridPoint2 getFormationPosition(int squadMemberIndex, GridPoint2 centralLocation, GameContext gameContext, int totalSquadMembers) {
		int numRows = (int) Math.ceil(Math.sqrt(totalSquadMembers));
		int numColumns = (int)Math.ceil((float)totalSquadMembers / (float)numRows);

		GridPoint2 offsetFromCenter = new GridPoint2(-numColumns / 2, -numRows / 2);

		MapTile initialTile = gameContext.getAreaMap().getTile(centralLocation);
		if (initialTile == null || !initialTile.isNavigable(null)) {
			return null;
		}
		initialTile = gameContext.getAreaMap().getTile(offsetFromCenter.add(centralLocation));
		if (initialTile == null) {
			return null;
		}

		List<GridPoint2> availablePositions = getAvailablePositions(initialTile, gameContext, squadMemberIndex, numColumns);
		if (availablePositions.size() > squadMemberIndex) {
			return availablePositions.get(squadMemberIndex);
		} else {
			return null;
		}
	}


	private List<GridPoint2> getAvailablePositions(MapTile initialTile, GameContext gameContext, int positionsRequired, int numColumns) {
		List<GridPoint2> availablePositions = new ArrayList<>();
		int attempts = 0; // used to ensure we don't create an infinite loop
		for (int yPosition = initialTile.getTileY(); attempts < positionsRequired + 200; yPosition += getSpacing()) {
			for (int xPosition = initialTile.getTileX(); xPosition < initialTile.getTileX() + (numColumns * getSpacing()); xPosition += getSpacing()) {
				MapTile tileAtPosition = gameContext.getAreaMap().getTile(xPosition, yPosition);

				if (tileAtPosition != null && tileAtPosition.getRegionId() == initialTile.getRegionId() && tileAtPosition.isNavigable(null)) {
					availablePositions.add(tileAtPosition.getTilePosition());
				}

				attempts++;
				if (availablePositions.size() > positionsRequired) {
					return availablePositions;
				}
			}
		}
		return availablePositions;
	}

	@Override
	public String toString() {
		return getFormationName();
	}
}
