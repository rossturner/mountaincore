package technology.rocketjump.mountaincore.assets.entities.vehicle;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.mountaincore.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleEntityAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ProvidedBy(VehicleEntityAssetDictionaryProvider.class)
@Singleton
public class VehicleEntityAssetDictionary {

	private final Map<String, VehicleEntityAsset> assetsByName = new HashMap<>();
	private final VehicleTypeDictionary vehicleTypeDictionary;
	private VehicleEntityAssetsByAssetType byAssetType;

	public VehicleEntityAssetDictionary(List<VehicleEntityAsset> completeAssetList, VehicleTypeDictionary vehicleTypeDictionary) {
		this.vehicleTypeDictionary = vehicleTypeDictionary;

		for (VehicleEntityAsset asset : completeAssetList) {
			assetsByName.put(asset.getUniqueName(), asset);
		}
		rebuild();
	}

	public VehicleEntityAsset getByUniqueName(String uniqueAssetName) {
		VehicleEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return null;
		}
	}

	public VehicleEntityAsset getVehicleEntityAsset(EntityAssetType assetType, VehicleEntityAttributes attributes) {
		return byAssetType.get(assetType, attributes);
	}

	public List<VehicleEntityAsset> getAllMatchingAssets(EntityAssetType assetType, VehicleEntityAttributes attributes) {
		return byAssetType.getAll(assetType, attributes);
	}

	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}


	public void add(VehicleEntityAsset asset) {
		assetsByName.put(asset.getUniqueName(), asset);
		rebuild();
	}

	public void rebuild() {
		byAssetType = new VehicleEntityAssetsByAssetType(vehicleTypeDictionary);
		for (VehicleEntityAsset asset : assetsByName.values()) {
			byAssetType.add(asset);
		}
	}

}
