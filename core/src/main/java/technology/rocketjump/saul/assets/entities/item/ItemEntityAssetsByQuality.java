package technology.rocketjump.saul.assets.entities.item;

import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;

import java.util.EnumMap;
import java.util.List;

public class ItemEntityAssetsByQuality {

	private EnumMap<ItemQuality, ItemEntityAssetsBySize> byItemQuality = new EnumMap<>(ItemQuality.class);

	public ItemEntityAssetsByQuality() {
		for (ItemQuality itemQuality : ItemQuality.values()) {
			byItemQuality.put(itemQuality, new ItemEntityAssetsBySize());
		}
	}

	public void add(ItemEntityAsset asset) {
		List<ItemQuality> itemQualities = asset.getItemQualities();
		if (itemQualities == null || itemQualities.isEmpty()) {
			// Add to all
			for (ItemQuality itemQuality : ItemQuality.values()) {
				byItemQuality.get(itemQuality).add(asset);
			}
		} else {
			itemQualities.forEach(iq -> byItemQuality.get(iq).add(asset));
		}
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemQuality itemQuality = attributes.getItemQuality();
		if (itemQuality == null) {
			itemQuality = ItemQuality.STANDARD;
		}
		return byItemQuality.get(itemQuality).get(attributes);
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemQuality itemQuality = attributes.getItemQuality();
		if (itemQuality == null) {
			itemQuality = ItemQuality.STANDARD;
		}
		return byItemQuality.get(itemQuality).getAll(attributes);
	}

	public ItemEntityAssetsBySize getSizeMapByQuality(ItemQuality itemQuality) {
		return byItemQuality.get(itemQuality);
	}
}
