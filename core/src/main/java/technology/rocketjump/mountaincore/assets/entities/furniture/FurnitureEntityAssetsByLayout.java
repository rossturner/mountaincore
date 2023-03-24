package technology.rocketjump.mountaincore.assets.entities.furniture;

import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetsByLayout {

	private Map<String, FurnitureEntityAssetsByMaterialType> byLayoutName = new HashMap<>();

	public void add(FurnitureEntityAsset asset) {
		String layoutName = asset.getFurnitureLayoutName();
		if (layoutName == null) {
			throw new RuntimeException(asset.getUniqueName() + " must have a layout specified");
		} else {
			byLayoutName.computeIfAbsent(layoutName, a -> new FurnitureEntityAssetsByMaterialType()).add(asset);
		}
	}

	public FurnitureEntityAsset get(FurnitureEntityAttributes attributes) {
		String layoutName = attributes.getCurrentLayout().getUniqueName();
		FurnitureEntityAssetsByMaterialType childMap = byLayoutName.get(layoutName);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<FurnitureEntityAsset> getAll(FurnitureEntityAttributes attributes) {
		String layoutName = attributes.getCurrentLayout().getUniqueName();
		FurnitureEntityAssetsByMaterialType childMap = byLayoutName.get(layoutName);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}
}
