package technology.rocketjump.saul.assets.entities.item;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.materials.model.GameMaterial.NULL_MATERIAL;

/**
 * This asset map works a little differently to the others - there is not an entry for every material - so it is best placed at the end of the tree nodes to avoid issues
 */
public class ItemEntityAssetsByMaterial {

	private Map<String, List<ItemEntityAsset>> materialNameMap = new HashMap<>();
	private List<ItemEntityAsset> completeList = new ArrayList<>();

	public ItemEntityAssetsByMaterial() {
		materialNameMap.put(NULL_MATERIAL.getMaterialName(), new ArrayList<>());
	}

	public void add(ItemEntityAsset asset) {
		List<String> applicableMaterials = asset.getApplicableMaterialNames();
		if (applicableMaterials != null && !applicableMaterials.isEmpty()) {
			for (String applicableMaterialName : applicableMaterials) {
				materialNameMap.computeIfAbsent(applicableMaterialName, a -> new ArrayList<>()).add(asset);
			}
		} else {
			materialNameMap.get(NULL_MATERIAL.getMaterialName()).add(asset);
		}
		completeList.add(asset);
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		GameMaterial material = attributes.getPrimaryMaterial();
		if (material == null || !materialNameMap.containsKey(material.getMaterialName())) {
			material = NULL_MATERIAL;
		}
		List<ItemEntityAsset> assets = materialNameMap.get(material.getMaterialName());
		if (assets.size() == 0) {
			Logger.error("Could not find applicable asset for " + attributes);
			return null;
		} else {
			return assets.get((Math.abs((int)attributes.getSeed())) % assets.size());
		}
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		GameMaterial material = attributes.getPrimaryMaterial();
		if (material == null || !materialNameMap.containsKey(material.getMaterialName())) {
			material = NULL_MATERIAL;
		}
		return materialNameMap.get(material.getMaterialName());
	}

	public List<ItemEntityAsset> all() {
		return completeList;
	}

}
