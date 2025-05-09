package technology.rocketjump.mountaincore.entities.model.physical.combat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.AmmoType;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.jobs.model.SkillType;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;

import static technology.rocketjump.mountaincore.jobs.SkillDictionary.UNARMED_COMBAT_SKILL;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaponInfo {

	private int range;
	private CombatDamageType damageType;
	private boolean modifiedByStrength;
	private int minDamage;
	private int maxDamage;
	private int armorNegation;
	private AmmoType requiresAmmoType;
	private boolean twoHanded;

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
		UNARMED.setRange(1);
		UNARMED.setDamageType(CombatDamageType.CRUSHING);
		UNARMED.setModifiedByStrength(true);
		UNARMED.setMinDamage(0);
		UNARMED.setMaxDamage(4);
	}
	
	@JsonIgnore
	public void initialise(String parentName, SoundAssetDictionary soundAssetDictionary, 
						   ParticleEffectTypeDictionary particleEffectTypeDictionary, SkillDictionary skillDictionary) {
		if (this.getFireWeaponSoundAssetName() != null) {
			this.setFireWeaponSoundAsset(soundAssetDictionary.getByName(this.getFireWeaponSoundAssetName()));
			if (this.getFireWeaponSoundAsset() == null) {
				Logger.error(String.format("Could not find sound asset with name %s for %s", this.getFireWeaponSoundAssetName(), parentName));
			}
		}

		if (this.getWeaponHitSoundAssetName() != null) {
			this.setWeaponHitSoundAsset(soundAssetDictionary.getByName(this.getWeaponHitSoundAssetName()));
			if (this.getWeaponHitSoundAsset() == null) {
				Logger.error(String.format("Could not find sound asset with name %s for item type %s", this.getWeaponHitSoundAssetName(), parentName));
			}
		}

		if (this.getWeaponMissSoundAssetName() != null) {
			this.setWeaponMissSoundAsset(soundAssetDictionary.getByName(this.getWeaponMissSoundAssetName()));
			if (this.getWeaponMissSoundAsset() == null) {
				Logger.error(String.format("Could not find sound asset with name %s for item type %s", this.getWeaponMissSoundAssetName(), parentName));
			}
		}

		if (this.getAnimatedSpriteEffectName() != null) {
			this.setAnimatedEffectType(particleEffectTypeDictionary.getByName(this.getAnimatedSpriteEffectName()));
			if (this.getAnimatedEffectType() == null) {
				Logger.error(String.format("Could not find particle effect with name %s for item type %s", this.getAnimatedSpriteEffectName(), parentName));
			} else if (this.getAnimatedEffectType().getAnimatedSpriteName() == null) {
				Logger.error(String.format("Particle effect %s is not an animated-sprite type particle effect, for %s",
						this.getAnimatedEffectType().getName(), parentName));
			}
		}

		if (this.getCombatSkillName() != null) {
			Skill combatSkill = skillDictionary.getByName(this.getCombatSkillName());
			if (combatSkill == null) {
				Logger.error("Could not find combat skill with name %s for item type %s", this.getCombatSkillName(), parentName);
			} else if (!combatSkill.getType().equals(SkillType.COMBAT_SKILL)) {
				Logger.error("Combat skill with name %s for item type %s is not a COMBAT_SKILL-type skill", this.getCombatSkillName(), parentName);
			} else {
				this.setCombatSkill(combatSkill);
			}
		}
	}

	public boolean isRanged() {
		return range > 2;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
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

	public int getArmorNegation() {
		return armorNegation;
	}

	public void setArmorNegation(int armorNegation) {
		this.armorNegation = armorNegation;
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

	public boolean isTwoHanded() {
		return twoHanded;
	}

	public void setTwoHanded(boolean twoHanded) {
		this.twoHanded = twoHanded;
	}
}
