package technology.rocketjump.saul.assets.entities.vehicle;

import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.saul.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleEntityAssetsByAssetType {

	private final Map<EntityAssetType, VehicleEntityAssetsByVehicleType> byAssetType = new HashMap<>();
	private final VehicleTypeDictionary vehicleTypeDictionary;

	public VehicleEntityAssetsByAssetType(VehicleTypeDictionary vehicleTypeDictionary) {
		this.vehicleTypeDictionary = vehicleTypeDictionary;
	}

	public void add(VehicleEntityAsset asset) {
		byAssetType.computeIfAbsent(asset.getType(), a -> new VehicleEntityAssetsByVehicleType(vehicleTypeDictionary)).add(asset);
	}

	public VehicleEntityAsset get(EntityAssetType entityAssetType, VehicleEntityAttributes attributes) {
		VehicleEntityAssetsByVehicleType childMap = byAssetType.get(entityAssetType);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<VehicleEntityAsset> getAll(EntityAssetType entityAssetType, VehicleEntityAttributes attributes) {
		VehicleEntityAssetsByVehicleType childMap = byAssetType.get(entityAssetType);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
