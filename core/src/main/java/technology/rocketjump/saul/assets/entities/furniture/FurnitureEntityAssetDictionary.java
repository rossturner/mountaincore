package technology.rocketjump.saul.assets.entities.furniture;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ProvidedBy(FurnitureEntityAssetDictionaryProvider.class)
@Singleton
public class FurnitureEntityAssetDictionary {

	private final Map<String, FurnitureEntityAsset> assetsByName = new HashMap<>();
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private FurnitureEntityAssetsByAssetType byAssetType;

	public FurnitureEntityAssetDictionary(List<FurnitureEntityAsset> completeAssetList, FurnitureTypeDictionary furnitureTypeDictionary) {
		this.furnitureTypeDictionary = furnitureTypeDictionary;

		for (FurnitureEntityAsset asset : completeAssetList) {
			assetsByName.put(asset.getUniqueName(), asset);
		}
		rebuild();
	}

	public FurnitureEntityAsset getByUniqueName(String uniqueAssetName) {
		FurnitureEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return null;
		}
	}

	public FurnitureEntityAsset getFurnitureEntityAsset(EntityAssetType assetType, FurnitureEntityAttributes attributes) {
		return byAssetType.get(assetType, attributes);
	}

	public List<FurnitureEntityAsset> getAllMatchingAssets(EntityAssetType assetType, FurnitureEntityAttributes attributes) {
		return byAssetType.getAll(assetType, attributes);
	}

	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}


	public void add(FurnitureEntityAsset asset) {
		assetsByName.put(asset.getUniqueName(), asset);
		rebuild();
	}

	public void rebuild() {
		byAssetType = new FurnitureEntityAssetsByAssetType(furnitureTypeDictionary);
		for (FurnitureEntityAsset asset : assetsByName.values()) {
			byAssetType.add(asset);
		}
	}

}
