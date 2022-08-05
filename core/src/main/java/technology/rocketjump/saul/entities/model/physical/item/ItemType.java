package technology.rocketjump.saul.entities.model.physical.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.jobs.model.CraftingType;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.misc.Name;
import technology.rocketjump.saul.rooms.StockpileGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemType {

	public static final double DEFAULT_HOURS_FOR_ITEM_TO_BECOME_UNUSED = 38.0;
	public static final ItemType UNARMED_WEAPON = new ItemType();
	static {
		UNARMED_WEAPON.setItemTypeName("UNARMED_WEAPON");
		UNARMED_WEAPON.setWeaponInfo(WeaponInfo.UNARMED);
	}

	@Name
	private String itemTypeName;

	private int maxStackSize = 1;
	private int maxHauledAtOnce; // or requiresHauling
	private List<GameMaterialType> materialTypes = new ArrayList<>();
	private GameMaterialType primaryMaterialType;

	private ItemHoldPosition holdPosition = ItemHoldPosition.IN_FRONT;
	private boolean impedesMovement = false;
	private boolean blocksMovement = false;
	private boolean equippedWhileWorkingOnJob = true; // Might need replacing with "can be shown hauling" property
	private double hoursInInventoryUntilUnused = DEFAULT_HOURS_FOR_ITEM_TO_BECOME_UNUSED;

	private List<String> relatedCraftingTypeNames = new ArrayList<>();
	@JsonIgnore
	private List<CraftingType> relatedCraftingTypes = new ArrayList<>();

	private String stockpileGroupName;
	@JsonIgnore
	private StockpileGroup stockpileGroup;

	private Map<String, List<String>> tags = new HashMap<>();
	@JsonIgnore
	private List<Tag> processedTags = new ArrayList<>();

	private String placementSoundAssetName;
	@JsonIgnore
	private SoundAsset placementSoundAsset;

	private String consumeSoundAssetName;
	@JsonIgnore
	private SoundAsset consumeSoundAsset;

	private WeaponInfo weaponInfo;
	private AmmoType isAmmoType;

	private boolean describeAsMaterialOnly;

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public int getMaxStackSize() {
		return maxStackSize;
	}

	public void setMaxStackSize(int maxStackSize) {
		this.maxStackSize = maxStackSize;
	}

	public int getMaxHauledAtOnce() {
		return maxHauledAtOnce;
	}

	public void setMaxHauledAtOnce(int maxHauledAtOnce) {
		this.maxHauledAtOnce = maxHauledAtOnce;
	}

	public boolean impedesMovement() {
		return impedesMovement;
	}

	public void setImpedesMovement(boolean impedesMovement) {
		this.impedesMovement = impedesMovement;
	}

	public boolean blocksMovement() {
		return blocksMovement;
	}

	public void setBlocksMovement(boolean blocksMovement) {
		this.blocksMovement = blocksMovement;
	}

	public ItemHoldPosition getHoldPosition() {
		return holdPosition;
	}

	public void setHoldPosition(ItemHoldPosition holdPosition) {
		this.holdPosition = holdPosition;
	}

	public String getConsumeSoundAssetName() {
		return consumeSoundAssetName;
	}

	public void setConsumeSoundAssetName(String consumeSoundAssetName) {
		this.consumeSoundAssetName = consumeSoundAssetName;
	}

	public SoundAsset getConsumeSoundAsset() {
		return consumeSoundAsset;
	}

	public void setConsumeSoundAsset(SoundAsset consumeSoundAsset) {
		this.consumeSoundAsset = consumeSoundAsset;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ItemType itemType = (ItemType) o;
		return itemTypeName.equals(itemType.itemTypeName);
	}

	@Override
	public int hashCode() {
		return itemTypeName.hashCode();
	}

	@Override
	public String toString() {
		return itemTypeName;
	}

	public List<GameMaterialType> getMaterialTypes() {
		return materialTypes;
	}

	public void setMaterialTypes(List<GameMaterialType> materialTypes) {
		this.materialTypes = materialTypes;
	}

	public GameMaterialType getPrimaryMaterialType() {
		return primaryMaterialType;
	}

	public void setPrimaryMaterialType(GameMaterialType primaryMaterialType) {
		this.primaryMaterialType = primaryMaterialType;
	}

	public boolean isImpedesMovement() {
		return impedesMovement;
	}

	public boolean isBlocksMovement() {
		return blocksMovement;
	}

	public List<String> getRelatedCraftingTypeNames() {
		return relatedCraftingTypeNames;
	}

	public void setRelatedCraftingTypeNames(List<String> relatedCraftingTypeNames) {
		this.relatedCraftingTypeNames = relatedCraftingTypeNames;
	}

	public List<CraftingType> getRelatedCraftingTypes() {
		return relatedCraftingTypes;
	}

	public void setRelatedCraftingTypes(List<CraftingType> relatedCraftingTypes) {
		this.relatedCraftingTypes = relatedCraftingTypes;
	}

	public String getI18nKey() {
		if (itemTypeName == null) {
			return null;
		}
		return itemTypeName.toUpperCase().replaceAll("-", ".").replaceAll(" ", "_");
	}

	public boolean isEquippedWhileWorkingOnJob() {
		return equippedWhileWorkingOnJob;
	}

	public void setEquippedWhileWorkingOnJob(boolean equippedWhileWorkingOnJob) {
		this.equippedWhileWorkingOnJob = equippedWhileWorkingOnJob;
	}

	public String getStockpileGroupName() {
		return stockpileGroupName;
	}

	public void setStockpileGroupName(String stockpileGroupName) {
		this.stockpileGroupName = stockpileGroupName;
	}

	public StockpileGroup getStockpileGroup() {
		return stockpileGroup;
	}

	public void setStockpileGroup(StockpileGroup stockpileGroup) {
		this.stockpileGroup = stockpileGroup;
	}

	public double getHoursInInventoryUntilUnused() {
		return hoursInInventoryUntilUnused;
	}

	public void setHoursInInventoryUntilUnused(double hoursInInventoryUntilUnused) {
		this.hoursInInventoryUntilUnused = hoursInInventoryUntilUnused;
	}

	public String getPlacementSoundAssetName() {
		return placementSoundAssetName;
	}

	public void setPlacementSoundAssetName(String placementSoundAssetName) {
		this.placementSoundAssetName = placementSoundAssetName;
	}

	public SoundAsset getPlacementSoundAsset() {
		return placementSoundAsset;
	}

	public void setPlacementSoundAsset(SoundAsset placementSoundAsset) {
		this.placementSoundAsset = placementSoundAsset;
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}

	public void setProcessedTags(List<Tag> processedTags) {
		this.processedTags = processedTags;
	}

	public List<Tag> getProcessedTags() {
		return processedTags;
	}

	public boolean isDescribeAsMaterialOnly() {
		return describeAsMaterialOnly;
	}

	public void setDescribeAsMaterialOnly(boolean describeAsMaterialOnly) {
		this.describeAsMaterialOnly = describeAsMaterialOnly;
	}

	public WeaponInfo getWeaponInfo() {
		return weaponInfo;
	}

	public void setWeaponInfo(WeaponInfo weaponInfo) {
		this.weaponInfo = weaponInfo;
	}

	public AmmoType getIsAmmoType() {
		return isAmmoType;
	}

	public void setIsAmmoType(AmmoType isAmmoType) {
		this.isAmmoType = isAmmoType;
	}

	@JsonIgnore
	public boolean isStackable() {
		return maxStackSize > 1 || maxHauledAtOnce > 1;
	}

}
