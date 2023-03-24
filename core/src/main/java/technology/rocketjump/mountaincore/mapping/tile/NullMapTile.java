package technology.rocketjump.mountaincore.mapping.tile;

import technology.rocketjump.mountaincore.entities.model.Entity;

import static technology.rocketjump.mountaincore.assets.model.FloorType.NULL_FLOOR;
import static technology.rocketjump.mountaincore.materials.model.GameMaterial.NULL_MATERIAL;

public class NullMapTile extends MapTile {

	public NullMapTile(int tileX, int tileY) {
		super(0L, tileX, tileY, NULL_FLOOR, NULL_MATERIAL);
	}

	@Override
	public boolean isNavigable(Entity requestingEntity) {
		return false;
	}

	@Override
	public boolean isNavigable(Entity requestingEntity, MapTile startingPoint) {
		return false;
	}


}
