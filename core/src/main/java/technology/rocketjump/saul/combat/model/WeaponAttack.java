package technology.rocketjump.saul.combat.model;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaponAttack implements ChildPersistable {

	private CombatDamageType damageType;
	private int minDamage;
	private int maxDamage;
	private int armorNegation;
	private ItemQuality weaponQuality = ItemQuality.STANDARD;
	private String weaponHitSoundAssetName;
	@JsonIgnore
	private SoundAsset weaponHitSoundAsset;
	private String weaponMissSoundAssetName;
	@JsonIgnore
	private SoundAsset weaponMissSoundAsset;
	private boolean modifiedByStrength;

	public WeaponAttack() {

	}

	public WeaponAttack(WeaponInfo weaponInfo, ItemQuality weaponQuality) {
		this.damageType = weaponInfo.getDamageType();
		this.minDamage = weaponInfo.getMinDamage();
		this.maxDamage = weaponInfo.getMaxDamage();
		this.armorNegation = weaponInfo.getArmorNegation();
		this.weaponQuality = weaponQuality;
		this.weaponHitSoundAsset = weaponInfo.getWeaponHitSoundAsset();
		this.weaponMissSoundAsset = weaponInfo.getWeaponMissSoundAsset();
		this.modifiedByStrength = weaponInfo.isModifiedByStrength();
	}

	public CombatDamageType getDamageType() {
		return damageType;
	}

	public void setDamageType(CombatDamageType damageType) {
		this.damageType = damageType;
	}

	public int getMinDamage() {
		return minDamage;
	}

	public void setMinDamage(int minDamage) {
		this.minDamage = minDamage;
	}

	public int getMaxDamage() {
		return maxDamage;
	}

	public void setMaxDamage(int maxDamage) {
		this.maxDamage = maxDamage;
	}

	public ItemQuality getWeaponQuality() {
		return weaponQuality;
	}

	public void setWeaponQuality(ItemQuality weaponQuality) {
		this.weaponQuality = weaponQuality;
	}
	public SoundAsset getWeaponHitSoundAsset() {
		return weaponHitSoundAsset;
	}

	public void setWeaponHitSoundAsset(SoundAsset weaponHitSoundAsset) {
		this.weaponHitSoundAsset = weaponHitSoundAsset;
	}

	public String getWeaponMissSoundAssetName() {
		return weaponMissSoundAssetName;
	}

	public void setWeaponMissSoundAssetName(String weaponMissSoundAssetName) {
		this.weaponMissSoundAssetName = weaponMissSoundAssetName;
	}

	public SoundAsset getWeaponMissSoundAsset() {
		return weaponMissSoundAsset;
	}

	public void setWeaponMissSoundAsset(SoundAsset weaponMissSoundAsset) {
		this.weaponMissSoundAsset = weaponMissSoundAsset;
	}

	public String getWeaponHitSoundAssetName() {
		return weaponHitSoundAssetName;
	}

	public void setWeaponHitSoundAssetName(String weaponHitSoundAssetName) {
		this.weaponHitSoundAssetName = weaponHitSoundAssetName;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("damageType", damageType.name());
		asJson.put("minDamage", minDamage);
		asJson.put("maxDamage", maxDamage);
		asJson.put("armorNegation", armorNegation);
		if (!weaponQuality.equals(ItemQuality.STANDARD)) {
			asJson.put("weaponQuality", weaponQuality.name());
		}
		if (weaponHitSoundAsset != null) {
			asJson.put("weaponHitSoundAssetName", weaponHitSoundAsset.getName());
		}
		if (weaponMissSoundAsset != null) {
			asJson.put("weaponMissSoundAssetName", weaponMissSoundAsset.getName());
		}
		if (modifiedByStrength) {
			asJson.put("modifiedByStrength", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.damageType = EnumParser.getEnumValue(asJson, "damageType", CombatDamageType.class, CombatDamageType.CRUSHING);
		this.minDamage = asJson.getIntValue("minDamage");
		this.maxDamage = asJson.getIntValue("maxDamage");
		this.armorNegation = asJson.getIntValue("armorNegation");
		this.weaponQuality = EnumParser.getEnumValue(asJson, "weaponQuality", ItemQuality.class, ItemQuality.STANDARD);

		this.weaponHitSoundAssetName = asJson.getString("weaponHitSoundAssetName");
		if (weaponHitSoundAssetName != null) {
			this.weaponHitSoundAsset = relatedStores.soundAssetDictionary.getByName(weaponHitSoundAssetName);
			if (this.weaponHitSoundAsset == null) {
				throw new InvalidSaveException("Could not find sound asset with name " + weaponHitSoundAssetName);
			}
		}

		this.weaponMissSoundAssetName = asJson.getString("weaponMissSoundAssetName");
		if (weaponMissSoundAssetName != null) {
			this.weaponMissSoundAsset = relatedStores.soundAssetDictionary.getByName(weaponMissSoundAssetName);
			if (this.weaponMissSoundAsset == null) {
				throw new InvalidSaveException("Could not find sound asset with name " + weaponMissSoundAssetName);
			}
		}

		this.modifiedByStrength = asJson.getBooleanValue("modifiedByStrength");
	}

	public boolean isModifiedByStrength() {
		return modifiedByStrength;
	}

	public void setModifiedByStrength(boolean modifiedByStrength) {
		this.modifiedByStrength = modifiedByStrength;
	}

	public int getArmorNegation() {
		return armorNegation;
	}

	public void setArmorNegation(int armorNegation) {
		this.armorNegation = armorNegation;
	}
}
