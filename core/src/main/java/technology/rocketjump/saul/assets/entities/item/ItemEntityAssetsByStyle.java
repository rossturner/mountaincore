package technology.rocketjump.saul.assets.entities.item;

import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemStyle;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;

import java.util.EnumMap;
import java.util.List;

public class ItemEntityAssetsByStyle {

	private EnumMap<ItemStyle, ItemEntityAssetsByMaterial> styleMap = new EnumMap<>(ItemStyle.class);

	public ItemEntityAssetsByStyle() {
		for (ItemStyle itemStyle : ItemStyle.values()) {
			styleMap.put(itemStyle, new ItemEntityAssetsByMaterial());
		}
	}

	public void add(ItemEntityAsset asset) {
		ItemStyle itemStyle = asset.getItemStyle();
		if (itemStyle == null) {
			// Add to all
			for (ItemStyle style : ItemStyle.values()) {
				styleMap.get(style).add(asset);
			}
		} else {
			styleMap.get(itemStyle).add(asset);
		}
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemStyle itemStyle = attributes.getItemStyle();
		if (itemStyle == null) {
			itemStyle = ItemStyle.DEFAULT;
		}
		return styleMap.get(itemStyle).get(attributes);
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemStyle itemStyle = attributes.getItemStyle();
		if (itemStyle == null) {
			itemStyle = ItemStyle.DEFAULT;
		}
		return styleMap.get(itemStyle).getAll(attributes);
	}

	public List<ItemEntityAsset> getByStyle(ItemStyle itemStyle) {
		return styleMap.get(itemStyle).all();
	}
}
