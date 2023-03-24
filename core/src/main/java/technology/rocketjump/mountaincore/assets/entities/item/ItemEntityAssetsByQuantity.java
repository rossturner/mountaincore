package technology.rocketjump.mountaincore.assets.entities.item;

import com.badlogic.gdx.utils.IntMap;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;

import java.util.List;

public class ItemEntityAssetsByQuantity {

	private static final int MAX_TRACKED_QUANTITY = 32;
	private IntMap<ItemEntityAssetsByAssetType> quantityMap = new IntMap<>();

	private static final int MIN_QUANTITY = 0;

	public ItemEntityAssetsByQuantity(ItemTypeDictionary itemTypeDictionary) {
		for (int q = MIN_QUANTITY; q <= MAX_TRACKED_QUANTITY; q++) {
			quantityMap.put(q, new ItemEntityAssetsByAssetType(itemTypeDictionary));
		}
	}

	public void add(ItemEntityAsset asset) {
		for (int cursor = asset.getMinQuantity(); cursor <= asset.getMaxQuantity(); cursor++) {
			if (cursor > MAX_TRACKED_QUANTITY) {
				break;
			}
			quantityMap.get(cursor).add(asset);
		}
	}

	public ItemEntityAsset get(EntityAssetType assetType, ItemEntityAttributes attributes) {
		if (attributes.getQuantity() > MAX_TRACKED_QUANTITY) {
			return quantityMap.get(MAX_TRACKED_QUANTITY).get(assetType, attributes);
		} else {
			return quantityMap.get(attributes.getQuantity()).get(assetType, attributes);
		}
	}

	public List<ItemEntityAsset> getAll(EntityAssetType assetType, ItemEntityAttributes attributes) {
		if (attributes.getQuantity() > MAX_TRACKED_QUANTITY) {
			return quantityMap.get(MAX_TRACKED_QUANTITY).getAll(assetType, attributes);
		} else {
			return quantityMap.get(attributes.getQuantity()).getAll(assetType, attributes);
		}
	}

}
