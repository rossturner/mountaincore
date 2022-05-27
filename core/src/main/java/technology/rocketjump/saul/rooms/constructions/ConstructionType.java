package technology.rocketjump.saul.rooms.constructions;

public enum ConstructionType {

	FURNITURE_CONSTRUCTION(FurnitureConstruction.class),
	DOORWAY_CONSTRUCTION(DoorwayConstruction.class),
	WALL_CONSTRUCTION(WallConstruction.class),
	BRIDGE_CONSTRUCTION(BridgeConstruction.class);

	public final Class<? extends Construction> classType;

	ConstructionType(Class<? extends Construction> classType) {
		this.classType = classType;
	}
}
