package technology.rocketjump.saul.entities.model.physical.furniture;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.misc.Name;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

public class FurnitureLayout {

	@Name
	private String uniqueName;
	private String rotatesToName;
	@JsonIgnore
	private FurnitureLayout rotatesTo;

	private List<GridPoint2> extraTiles = new ArrayList<>();
	private List<FurnitureLayout.Workspace> workspaces = new ArrayList<>();
	private List<FurnitureLayout.SpecialTile> specialTiles = new ArrayList<>();

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public String getRotatesToName() {
		return rotatesToName;
	}

	public void setRotatesToName(String rotatesToName) {
		this.rotatesToName = rotatesToName;
	}

	public FurnitureLayout getRotatesTo() {
		return rotatesTo;
	}

	public void setRotatesTo(FurnitureLayout rotatesTo) {
		this.rotatesTo = rotatesTo;
	}

	public List<GridPoint2> getExtraTiles() {
		return extraTiles;
	}

	public void setExtraTiles(List<GridPoint2> extraTiles) {
		this.extraTiles = extraTiles;
	}

	public List<Workspace> getWorkspaces() {
		return workspaces;
	}

	public void setWorkspaces(List<Workspace> workspaces) {
		this.workspaces = workspaces;
	}

	public List<SpecialTile> getSpecialTiles() {
		return specialTiles;
	}

	public void setSpecialTiles(List<SpecialTile> specialTiles) {
		this.specialTiles = specialTiles;
	}

	public int getWidth() {
		int minX = 0;
		int maxX = 0;
		for (GridPoint2 extraTile : extraTiles) {
			if (extraTile.x < minX) {
				minX = extraTile.x;
			}
			if (extraTile.x > maxX) {
				maxX = extraTile.x;
			}
		}
		return maxX - minX + 1;
	}

	public int getHeight() {
		int minY = 0;
		int maxY = 0;
		for (GridPoint2 extraTile : extraTiles) {
			if (extraTile.y < minY) {
				minY = extraTile.y;
			}
			if (extraTile.y > maxY) {
				maxY = extraTile.y;
			}
		}
		return maxY - minY + 1;
	}

	public static class Workspace {

		private GridPoint2 location;
		private GridPoint2 accessedFrom;

		public GridPoint2 getLocation() {
			return location;
		}

		public void setLocation(GridPoint2 location) {
			this.location = location;
		}

		public GridPoint2 getAccessedFrom() {
			return accessedFrom;
		}

		public void setAccessedFrom(GridPoint2 accessedFrom) {
			this.accessedFrom = accessedFrom;
		}

		@Override
		public String toString() {
			return "Location: " + location + ", Accessed from " + accessedFrom;
		}
	}


	public static FurnitureLayout.Workspace getNearestNavigableWorkspace(Entity furnitureEntity, TiledMap areaMap, GridPoint2 origin) {
		if (furnitureEntity == null) {
			Logger.error("Null entity passed to getNearestNavigableWorkspace");
			return null;
		}
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();

		GridPoint2 furniturePosition = toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition());
		List<FurnitureLayout.Workspace> navigableWorkspaces = new ArrayList<>();
		for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
			GridPoint2 accessedFromLocation = furniturePosition.cpy().add(workspace.getAccessedFrom());
			MapTile accessedFromTile = areaMap.getTile(accessedFromLocation);
			if (accessedFromTile != null && accessedFromTile.isNavigable(null)) {
				FurnitureLayout.Workspace worldPositionedWorkspace = new FurnitureLayout.Workspace();
				worldPositionedWorkspace.setLocation(furniturePosition.cpy().add(workspace.getLocation()));
				worldPositionedWorkspace.setAccessedFrom(furniturePosition.cpy().add(workspace.getAccessedFrom()));
				navigableWorkspaces.add(worldPositionedWorkspace);
			}
		}

		if (navigableWorkspaces.isEmpty()) {
			return null;
		} else {
			return navigableWorkspaces.stream().min((o1, o2) -> {
				float distanceTo1 = o1.getAccessedFrom().dst2(origin);
				float distanceTo2 = o2.getAccessedFrom().dst2(origin);
				return Math.round(distanceTo1 - distanceTo2);
			}).get();
		}
	}

	public static FurnitureLayout.Workspace getAnyNavigableWorkspace(Entity furnitureEntity, TiledMap areaMap) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();

		GridPoint2 furniturePosition = toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition());
		List<FurnitureLayout.Workspace> workspaces = attributes.getCurrentLayout().getWorkspaces();
		Collections.shuffle(workspaces);
		for (FurnitureLayout.Workspace workspace : workspaces) {
			GridPoint2 accessedFromLocation = furniturePosition.cpy().add(workspace.getAccessedFrom());
			MapTile accessedFromTile = areaMap.getTile(accessedFromLocation);
			if (accessedFromTile != null && accessedFromTile.isNavigable(null)) {
				FurnitureLayout.Workspace worldPositionedWorkspace = new FurnitureLayout.Workspace();
				worldPositionedWorkspace.setLocation(furniturePosition.cpy().add(workspace.getLocation()));
				worldPositionedWorkspace.setAccessedFrom(furniturePosition.cpy().add(workspace.getAccessedFrom()));
				return worldPositionedWorkspace;
			}
		}

		return null;
	}

	public static class SpecialTile {

		private GridPoint2 location;
		private SpecialTileRequirment requirement;

		public enum SpecialTileRequirment {

			IS_RIVER(mapTile -> mapTile.getFloor().isRiverTile(), HexColors.get("#26e1ed"));

			public final TileCheck tileCheck;
			public final Color color;

			private SpecialTileRequirment(TileCheck tileCheck, Color color) {
				this.tileCheck = tileCheck;
				this.color = color;
			}

			public interface TileCheck {
				boolean isValid(MapTile mapTile);
			}

		}

		public GridPoint2 getLocation() {
			return location;
		}

		public void setLocation(GridPoint2 location) {
			this.location = location;
		}

		public SpecialTileRequirment getRequirement() {
			return requirement;
		}

		public void setRequirement(SpecialTileRequirment requirement) {
			this.requirement = requirement;
		}

		@Override
		public String toString() {
			return "Location: " + location + ", Requirement " + requirement.name();
		}
	}

	@Override
	public String toString() {
		return uniqueName;
	}
}
