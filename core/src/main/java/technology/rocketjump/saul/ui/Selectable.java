package technology.rocketjump.saul.ui;

import technology.rocketjump.saul.doors.Doorway;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.rooms.Bridge;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.constructions.Construction;

import java.util.Objects;

/**
 * This holds and represents something which can be clicked on and selected by the UI in the game world
 */
public class Selectable implements Comparable<Selectable> {

	public final SelectableType type;
	private Entity entity;

	private Construction construction;
	private Room room;
	private Squad squad;
	private Bridge bridge;
	private MapTile tile;
	private Doorway doorway;
	private int distanceFromCursor;

	public Selectable(Entity entity, float distanceFromCursor) {
		this.type = SelectableType.ENTITY;
		this.entity = entity;
		this.distanceFromCursor = (int)(distanceFromCursor * 1000f);
	}

	public Selectable(Construction construction) {
		this.type = SelectableType.CONSTRUCTION;
		this.construction = construction;
	}

	public Selectable(Room room) {
		this.type = SelectableType.ROOM;
		this.room = room;
	}

	public Selectable(Squad squad) {
		this.type = SelectableType.SQUAD;
		this.squad = squad;
	}

	public Selectable(Bridge bridge) {
		this.type = SelectableType.BRIDGE;
		this.bridge = bridge;
	}

	public Selectable(Doorway doorway) {
		this.type = SelectableType.DOORWAY;
		this.doorway = doorway;
	}

	public Selectable(MapTile tile) {
		this.type = SelectableType.TILE;
		this.tile = tile;
	}

	@Override
	public int compareTo(Selectable other) {
		return other.getSortValue() - this.getSortValue();
	}

	public boolean equals(Selectable other) {
		if (other == null) {
			return false;
		}
		if (this.type.equals(other.type)) {
			// Types are the same so compare on IDs
			return this.getId() == other.getId();
		} else {
			// Types are different so definitely not the same
			return false;
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || getClass() != other.getClass()) return false;
		return equals((Selectable)other);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId());
	}

	public long getId() {
		return switch (this.type) {
			case ENTITY -> this.entity.getId();
			case ROOM -> this.room.getRoomId();
			case BRIDGE -> this.bridge.getBridgeId();
			case TILE -> (this.tile.getTileX() * 10000L) + this.tile.getTileY();
			case DOORWAY -> this.doorway.getDoorEntity().getId();
			case CONSTRUCTION -> this.construction.getId();
			case SQUAD -> this.squad.getId();
		};
	}

	public Entity getEntity() {
		return entity;
	}

	public Construction getConstruction() {
		return construction;
	}

	public Room getRoom() {
		return room;
	}

	public Bridge getBridge() {
		return bridge;
	}

	public MapTile getTile() {
		return tile;
	}

	public Doorway getDoorway() {
		return doorway;
	}

	private int getSortValue() {
		int sort = type.sortOrder;
		if (this.type == SelectableType.ENTITY) {
			sort -= distanceFromCursor;
		}
		return sort;
	}

	public Squad getSquad() {
		return squad;
	}

	public enum SelectableType {

		SQUAD(6000),
		ENTITY(5000),
		CONSTRUCTION(4000),
		DOORWAY(3000),
		BRIDGE(2001),
		ROOM(2000),
		TILE(1000);

		private final int sortOrder;

		SelectableType(int sortOrder) {
			this.sortOrder = sortOrder;
		}
	}

}
