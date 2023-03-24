package technology.rocketjump.mountaincore.assets.entities.item.model;

import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemQuality;
import technology.rocketjump.mountaincore.misc.Name;

import java.util.ArrayList;
import java.util.List;

public class ItemEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private EntityAssetType type;
	private String itemTypeName;
	private int minQuantity = 1; // The fewest amount that this asset represents
	private int maxQuantity = 1; // The largest amount that this asset can represent
	private List<ItemQuality> itemQualities = new ArrayList<>();
	private List<ItemPlacement> itemPlacements = new ArrayList<>();
	private List<String> applicableMaterialNames = new ArrayList<>();

	public boolean matches(ItemEntityAttributes entityAttributes) {
		if (itemTypeName != null && !itemTypeName.equals(entityAttributes.getItemType().getItemTypeName())) {
			return false;
		}
		if (minQuantity > entityAttributes.getQuantity() || maxQuantity < entityAttributes.getQuantity()) {
			return false;
		}
		if (itemQualities != null && !itemQualities.isEmpty() && entityAttributes.getItemQuality() != null && !itemQualities.contains(entityAttributes.getItemQuality())) {
			return false;
		}
		if (applicableMaterialNames != null && !applicableMaterialNames.isEmpty() && !applicableMaterialNames.contains(entityAttributes.getPrimaryMaterial().getMaterialName())) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public List<ItemQuality> getItemQualities() {
		return itemQualities;
	}

	public void setItemQualities(List<ItemQuality> itemQualities) {
		this.itemQualities = itemQualities;
	}

	public int getMinQuantity() {
		return minQuantity;
	}

	public void setMinQuantity(int minQuantity) {
		this.minQuantity = minQuantity;
	}

	public int getMaxQuantity() {
		return maxQuantity;
	}

	public void setMaxQuantity(int maxQuantity) {
		this.maxQuantity = maxQuantity;
	}

	public List<ItemPlacement> getItemPlacements() {
		return itemPlacements;
	}

	public void setItemPlacements(List<ItemPlacement> itemPlacements) {
		this.itemPlacements = itemPlacements;
	}

	public List<String> getApplicableMaterialNames() {
		return applicableMaterialNames;
	}

	public void setApplicableMaterialNames(List<String> applicableMaterialNames) {
		this.applicableMaterialNames = applicableMaterialNames;
	}
}
