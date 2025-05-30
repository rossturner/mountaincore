package technology.rocketjump.mountaincore.assets.entities.wallcap.model;

import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.doors.DoorwayOrientation;
import technology.rocketjump.mountaincore.doors.DoorwaySize;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.misc.Name;

public class WallCapAssetDescriptor {

	@Name
	private String uniqueName;
	protected EntityAssetType type;
	private String wallTypeName;
	private GameMaterialType doorwayMaterialType;
	private DoorwayOrientation doorwayOrientation;
	private DoorwaySize doorwaySize;

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public String getWallTypeName() {
		return wallTypeName;
	}

	public void setWallTypeName(String wallTypeName) {
		this.wallTypeName = wallTypeName;
	}

	public GameMaterialType getDoorwayMaterialType() {
		return doorwayMaterialType;
	}

	public void setDoorwayMaterialType(GameMaterialType doorwayMaterialType) {
		this.doorwayMaterialType = doorwayMaterialType;
	}

	public DoorwayOrientation getDoorwayOrientation() {
		return doorwayOrientation;
	}

	public void setDoorwayOrientation(DoorwayOrientation doorwayOrientation) {
		this.doorwayOrientation = doorwayOrientation;
	}

	public DoorwaySize getDoorwaySize() {
		return doorwaySize;
	}

	public void setDoorwaySize(DoorwaySize doorwaySize) {
		this.doorwaySize = doorwaySize;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}
}
