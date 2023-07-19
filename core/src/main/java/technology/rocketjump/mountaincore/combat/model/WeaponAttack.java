package technology.rocketjump.mountaincore.combat.model;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemQuality;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaponAttack implements ChildPersistable {

	private CombatDamageType damageType;
	private int minDamage;
	private int maxDamage;
	private int armorNegation;
	private String weaponHitSoundAssetName;
	@JsonIgnore
	private SoundAsset weaponHitSoundAsset;
	private String weaponMissSoundAssetName;
	@JsonIgnore
	private SoundAsset weaponMissSoundAsset;
	private boolean modifiedByStrength;
	private boolean isRanged;

	public WeaponAttack() {

	}

	public WeaponAttack(WeaponInfo weaponInfo, ItemQuality weaponQuality, GameMaterial weaponMaterial) {
		this.damageType = weaponInfo.getDamageType();
		float damageScalar = weaponDamageScalar(weaponQuality, weaponMaterial, damageType, weaponInfo.isRanged());
		this.minDamage = Math.max(Math.round((float) weaponInfo.getMinDamage() * damageScalar), 0);
		this.maxDamage = Math.max(Math.round((float) weaponInfo.getMaxDamage() * damageScalar), 1);
		this.armorNegation = weaponInfo.getArmorNegation();
		this.weaponQuality = weaponQuality;
		this.weaponHitSoundAsset = weaponInfo.getWeaponHitSoundAsset();
		this.weaponMissSoundAsset = weaponInfo.getWeaponMissSoundAsset();
		this.modifiedByStrength = weaponInfo.isModifiedByStrength();
		this.isRanged = weaponInfo.isRanged();
	}

	private static float weaponDamageScalar(ItemQuality weaponQuality, GameMaterial weaponMaterial, CombatDamageType damageType, boolean isRanged) {
		if (!isRanged && weaponMaterial != null) {
			return weaponQuality.combatMultiplier * damageType.weaponDamageScalar(weaponMaterial.getHardness(), weaponMaterial.getWeight());
		}

		// else ranged
		return weaponQuality.combatMultiplier;
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

	public int getMaxDamage() {
		return maxDamage;
	}

	public SoundAsset getWeaponHitSoundAsset() {
		return weaponHitSoundAsset;
	}

	public String getWeaponMissSoundAssetName() {
		return weaponMissSoundAssetName;
	}

	public SoundAsset getWeaponMissSoundAsset() {
		return weaponMissSoundAsset;
	}

	public String getWeaponHitSoundAssetName() {
		return weaponHitSoundAssetName;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("damageType", damageType.name());
		asJson.put("minDamage", minDamage);
		asJson.put("maxDamage", maxDamage);
		asJson.put("armorNegation", armorNegation);
		if (weaponHitSoundAsset != null) {
			asJson.put("weaponHitSoundAssetName", weaponHitSoundAsset.getName());
		}
		if (weaponMissSoundAsset != null) {
			asJson.put("weaponMissSoundAssetName", weaponMissSoundAsset.getName());
		}
		if (modifiedByStrength) {
			asJson.put("modifiedByStrength", true);
		}
		if (isRanged) {
			asJson.put("isRanged", true);
		}
	}

	public boolean isModifiedByStrength() {
		return modifiedByStrength;
	}

	public int getArmorNegation() {
		return armorNegation;
	}

	public void setArmorNegation(int armorNegation) {
		this.armorNegation = armorNegation;
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.damageType = EnumParser.getEnumValue(asJson, "damageType", CombatDamageType.class, CombatDamageType.CRUSHING);
		this.minDamage = asJson.getIntValue("minDamage");
		this.maxDamage = asJson.getIntValue("maxDamage");
		this.armorNegation = asJson.getIntValue("armorNegation");

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
		this.isRanged = asJson.getBooleanValue("isRanged");
	}

}
