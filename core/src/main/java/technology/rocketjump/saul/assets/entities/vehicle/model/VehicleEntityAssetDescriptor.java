package technology.rocketjump.saul.assets.entities.vehicle.model;

import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.saul.misc.Name;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private String vehicleTypeName;
	private EntityAssetType type;

	private Map<String, List<String>> tags = new HashMap<>();

	public boolean matches(VehicleEntityAttributes entityAttributes) {
		if (vehicleTypeName != null && !vehicleTypeName.equals(entityAttributes.getVehicleType().getName())) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}


	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	@Override
	public String toString() {
		return getUniqueName();
	}

	public String getVehicleTypeName() {
		return vehicleTypeName;
	}

	public void setVehicleTypeName(String vehicleTypeName) {
		this.vehicleTypeName = vehicleTypeName;
	}
}
