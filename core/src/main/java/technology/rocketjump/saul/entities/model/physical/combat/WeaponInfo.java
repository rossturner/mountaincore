package technology.rocketjump.saul.entities.model.physical.combat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.model.physical.item.AmmoType;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import static technology.rocketjump.saul.jobs.SkillDictionary.UNARMED_COMBAT_SKILL;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaponInfo {

	private float range;
	private CombatDamageType damageType;
	private boolean modifiedByStrength;
	private int minDamage;
	private int maxDamage;
	private AmmoType requiresAmmoType;

	private String combatSkillName;
	@JsonIgnore
	private Skill combatSkill = UNARMED_COMBAT_SKILL;
	private String fireWeaponSoundAssetName;
	@JsonIgnore
	private SoundAsset fireWeaponSoundAsset;
	private String weaponHitSoundAssetName;
	@JsonIgnore
	private SoundAsset weaponHitSoundAsset;
	private String weaponMissSoundAssetName;
	@JsonIgnore
	private SoundAsset weaponMissSoundAsset;
	private String animatedSpriteEffectName;
	@JsonIgnore
	private ParticleEffectType animatedEffectType;


	public static WeaponInfo UNARMED = new WeaponInfo();
	static {
		UNARMED.setRange(1f);
		UNARMED.setDamageType(CombatDamageType.CRUSHING);
		UNARMED.setModifiedByStrength(true);
		UNARMED.setMinDamage(0);
		UNARMED.setMaxDamage(4);
	}

	public float getRange() {
		return range;
	}

	public void setRange(float range) {
		this.range = range;
	}

	public CombatDamageType getDamageType() {
		return damageType;
	}

	public void setDamageType(CombatDamageType damageType) {
		this.damageType = damageType;
	}

	public boolean isModifiedByStrength() {
		return modifiedByStrength;
	}

	public void setModifiedByStrength(boolean modifiedByStrength) {
		this.modifiedByStrength = modifiedByStrength;
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

	public AmmoType getRequiresAmmoType() {
		return requiresAmmoType;
	}

	public void setRequiresAmmoType(AmmoType requiresAmmoType) {
		this.requiresAmmoType = requiresAmmoType;
	}

	public String getCombatSkillName() {
		return combatSkillName;
	}

	public void setCombatSkillName(String combatSkillName) {
		this.combatSkillName = combatSkillName;
	}

	public Skill getCombatSkill() {
		return combatSkill;
	}

	public void setCombatSkill(Skill combatSkill) {
		this.combatSkill = combatSkill;
		this.combatSkillName = combatSkill.getName();
	}

	public String getFireWeaponSoundAssetName() {
		return fireWeaponSoundAssetName;
	}

	public void setFireWeaponSoundAssetName(String fireWeaponSoundAssetName) {
		this.fireWeaponSoundAssetName = fireWeaponSoundAssetName;
	}

	public SoundAsset getFireWeaponSoundAsset() {
		return fireWeaponSoundAsset;
	}

	public void setFireWeaponSoundAsset(SoundAsset fireWeaponSoundAsset) {
		this.fireWeaponSoundAsset = fireWeaponSoundAsset;
	}

	public String getWeaponHitSoundAssetName() {
		return weaponHitSoundAssetName;
	}

	public void setWeaponHitSoundAssetName(String weaponHitSoundAssetName) {
		this.weaponHitSoundAssetName = weaponHitSoundAssetName;
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

	public String getAnimatedSpriteEffectName() {
		return animatedSpriteEffectName;
	}

	public void setAnimatedSpriteEffectName(String animatedSpriteEffectName) {
		this.animatedSpriteEffectName = animatedSpriteEffectName;
	}

	public ParticleEffectType getAnimatedEffectType() {
		return animatedEffectType;
	}

	public void setAnimatedEffectType(ParticleEffectType animatedEffectType) {
		this.animatedEffectType = animatedEffectType;
	}
}
