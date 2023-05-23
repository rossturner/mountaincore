package technology.rocketjump.mountaincore.entities.model.physical.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.crafting.CraftingRecipeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.jobs.model.CraftingType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.misc.Name;
import technology.rocketjump.mountaincore.production.StockpileGroup;

import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemType {

	public static final double DEFAULT_HOURS_FOR_ITEM_TO_BECOME_UNUSED = 12.0;
	public static final ItemType UNARMED_WEAPON = new ItemType();
	static {
		UNARMED_WEAPON.setItemTypeName("UNARMED_WEAPON");
		UNARMED_WEAPON.setWeaponInfo(WeaponInfo.UNARMED);
	}

	@Name
	private String itemTypeName;

	private int maxStackSize = 1;
	private int maxHauledAtOnce = 1;
	private List<GameMaterialType> materialTypes = new ArrayList<>();
	private GameMaterialType primaryMaterialType;

	private int baseValuePerItem = 0;

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

	private List<String> specificMaterialNames = new ArrayList<>();
	@JsonIgnore
	private List<GameMaterial> specificMaterials = new ArrayList<>();

	private WeaponInfo weaponInfo;
	private AmmoType isAmmoType;
	private DefenseInfo defenseInfo;

	private boolean tradeExportable;
	private boolean tradeImportable;
	private boolean valueFixedToMaterial;

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

	public int getBaseValuePerItem() {
		return baseValuePerItem;
	}

	public void setBaseValuePerItem(int baseValuePerItem) {
		this.baseValuePerItem = baseValuePerItem;
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
		return itemTypeName.toUpperCase(Locale.ROOT).replaceAll("-", ".").replaceAll(" ", "_");
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

	public DefenseInfo getDefenseInfo() {
		return defenseInfo;
	}

	public void setDefenseInfo(DefenseInfo defenseInfo) {
		this.defenseInfo = defenseInfo;
	}

	public List<String> getSpecificMaterialNames() {
		return specificMaterialNames;
	}

	public void setSpecificMaterialNames(List<String> specificMaterialNames) {
		this.specificMaterialNames = specificMaterialNames;
	}

	public List<GameMaterial> getSpecificMaterials() {
		return specificMaterials;
	}

	public void setSpecificMaterials(List<GameMaterial> specificMaterials) {
		this.specificMaterials = specificMaterials;
	}

	@JsonIgnore
	public boolean isStackable() {
		return maxStackSize > 1 || maxHauledAtOnce > 1;
	}

	public boolean isTradeExportable() {
		return tradeExportable;
	}

	public void setTradeExportable(boolean tradeable) {
		tradeExportable = tradeable;
	}

	public boolean isTradeImportable() {
		return tradeImportable;
	}

	public void setTradeImportable(boolean tradeImportable) {
		this.tradeImportable = tradeImportable;
	}

	public boolean isValueFixedToMaterial() {
		return valueFixedToMaterial;
	}

	public void setValueFixedToMaterial(boolean valueFixedToMaterial) {
		this.valueFixedToMaterial = valueFixedToMaterial;
	}

	@JsonIgnore
	public List<GameMaterial> getSpecificallyAllowedMaterials(CraftingRecipeDictionary craftingRecipeDictionary) {
		Set<GameMaterial> allowedByCrafting = craftingRecipeDictionary.getAll().stream()
				.filter(r -> this.equals(r.getOutput().getItemType()))
				.map(r -> r.getOutput().getMaterial())
				.collect(Collectors.toSet());
		if (allowedByCrafting.isEmpty() || allowedByCrafting.contains(null)) {
			return specificMaterials;
		} else {
			return new ArrayList<>(Sets.union(allowedByCrafting, new HashSet<>(specificMaterials)));
		}
	}

}
