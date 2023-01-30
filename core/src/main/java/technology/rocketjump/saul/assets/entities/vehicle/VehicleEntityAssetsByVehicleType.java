package technology.rocketjump.saul.assets.entities.vehicle;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.saul.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleEntityAssetsByVehicleType {

	private final VehicleTypeDictionary vehicleTypeDictionary;
	private Map<VehicleType, List<VehicleEntityAsset>> byVehicleType = new HashMap<>();

	public VehicleEntityAssetsByVehicleType(VehicleTypeDictionary vehicleTypeDictionary) {
		this.vehicleTypeDictionary = vehicleTypeDictionary;
	}

	public void add(VehicleEntityAsset asset) {
		String vehicleTypeName = asset.getVehicleTypeName();
		VehicleType vehicleType = vehicleTypeDictionary.getByName(vehicleTypeName);
		if (vehicleType == null) {
			Logger.error("Unrecognised vehicle type name in " + asset.getUniqueName() + ": " + vehicleTypeName);
		} else {
			byVehicleType.computeIfAbsent(vehicleType, a -> new ArrayList<>()).add(asset);
		}
	}

	public VehicleEntityAsset get(VehicleEntityAttributes attributes) {
		List<VehicleEntityAsset> assets = byVehicleType.getOrDefault(attributes.getVehicleType(), List.of());
		if (assets.size() == 0) {
			Logger.error("Could not find applicable asset for " + attributes);
			return null;
		} else {
			return assets.get((Math.abs((int)attributes.getSeed())) % assets.size());
		}

	}

	public List<VehicleEntityAsset> getAll(VehicleEntityAttributes attributes) {
		return byVehicleType.getOrDefault(attributes.getVehicleType(), List.of());
	}

}
