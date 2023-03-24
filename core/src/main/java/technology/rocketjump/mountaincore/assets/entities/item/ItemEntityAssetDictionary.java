package technology.rocketjump.mountaincore.assets.entities.item;

import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ProvidedBy(ItemEntityAssetDictionaryProvider.class)
@Singleton
public class ItemEntityAssetDictionary {

	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final Map<String, ItemEntityAsset> assetsByName = new HashMap<>();
	private ItemEntityAssetsByQuantity quantityMap;

	public ItemEntityAssetDictionary(List<ItemEntityAsset> completeAssetList, EntityAssetTypeDictionary entityAssetTypeDictionary, ItemTypeDictionary itemTypeDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		for (ItemEntityAsset asset : completeAssetList) {
			assetsByName.put(asset.getUniqueName(), asset);
		}
		rebuild();
	}

	public void add(ItemEntityAsset itemEntityAsset) {
		assetsByName.put(itemEntityAsset.getUniqueName(), itemEntityAsset);
		rebuild();
	}

	public void rebuild() {
		this.quantityMap = new ItemEntityAssetsByQuantity(itemTypeDictionary);
		for (ItemEntityAsset asset : assetsByName.values()) {
			quantityMap.add(asset);
		}
	}

	public ItemEntityAsset getByUniqueName(String uniqueAssetName) {
		ItemEntityAsset asset = assetsByName.get(uniqueAssetName);
		if (asset != null) {
			return asset;
		} else {
			Logger.error("Could not find asset by name " + uniqueAssetName);
			return null;
		}
	}

	public ItemEntityAsset getItemEntityAsset(EntityAssetType assetType, ItemEntityAttributes attributes) {
		return quantityMap.get(assetType, attributes);
	}

	public List<ItemEntityAsset> getAllMatchingAssets(EntityAssetType assetType, ItemEntityAttributes attributes) {
		return quantityMap.getAll(assetType, attributes);
	}

	public Map<? extends String, ? extends EntityAsset> getAll() {
		return assetsByName;
	}

}
