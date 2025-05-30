package technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor;

import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.mountaincore.assets.editor.factory.ItemUIFactory;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;

import java.util.List;

import static technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder.toggle;
import static technology.rocketjump.mountaincore.jobs.SkillDictionary.UNARMED_COMBAT_SKILL;

public class WeaponInfoWidget extends VisTable {

	private static final ParticleEffectType NULL_PARTICLE_EFFECT = new ParticleEffectType();
	static {
		NULL_PARTICLE_EFFECT.setName("-none-");
	}

	public WeaponInfoWidget(WeaponInfo weaponInfo, SoundAssetDictionary soundAssetDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary,
							SkillDictionary skillDictionary) {
		this.columnDefaults(0).uniformX().left();
		this.columnDefaults(1).fillX().left();

		this.add(WidgetBuilder.label("Modified By Strength"));
		this.add(toggle(weaponInfo.isModifiedByStrength(), weaponInfo::setModifiedByStrength));
		this.row();

		this.add(WidgetBuilder.label("Min Damage"));
		this.add(WidgetBuilder.intSpinner(weaponInfo.getMinDamage(), 0, 100, weaponInfo::setMinDamage));
		this.row();

		this.add(WidgetBuilder.label("Max Damage"));
		this.add(WidgetBuilder.intSpinner(weaponInfo.getMaxDamage(), 0, 100, weaponInfo::setMaxDamage));
		this.row();

		this.add(WidgetBuilder.label("Armor Negation"));
		this.add(WidgetBuilder.intSpinner(weaponInfo.getArmorNegation(), 0, 100, weaponInfo::setArmorNegation));
		this.row();

		this.add(WidgetBuilder.label("Damage Type"));
		this.add(WidgetBuilder.select(weaponInfo.getDamageType(), CombatDamageType.values(), null, weaponInfo::setDamageType));
		this.row();

		this.add(WidgetBuilder.label("Range"));
		this.add(WidgetBuilder.intSpinner(weaponInfo.getRange(), 1, 200, weaponInfo::setRange));
		this.row();

		this.add(WidgetBuilder.label("Requires Ammo"));
		this.add(ItemUIFactory.ammoTypeSelect(weaponInfo.getRequiresAmmoType(), weaponInfo::setRequiresAmmoType));
		this.row();

		this.add(WidgetBuilder.label("Two handed"));
		this.add(toggle(weaponInfo.isTwoHanded(), weaponInfo::setTwoHanded));
		this.row();

		this.add(WidgetBuilder.label("Combat skill"));
		this.add(WidgetBuilder.select(weaponInfo.getCombatSkill(), skillDictionary.getAllCombatSkills(), UNARMED_COMBAT_SKILL, weaponInfo::setCombatSkill));
		this.row();

		this.add(WidgetBuilder.label("Fire Sound"));
		this.add(WidgetBuilder.select(weaponInfo.getFireWeaponSoundAsset(), soundAssetDictionary.getAll(), SoundAssetDictionary.NULL_SOUND_ASSET, soundAsset -> {
			weaponInfo.setFireWeaponSoundAsset(soundAsset);
			weaponInfo.setFireWeaponSoundAssetName(soundAsset.getName());
		}));
		this.row();

		this.add(WidgetBuilder.label("Hit Sound"));
		this.add(WidgetBuilder.select(weaponInfo.getWeaponHitSoundAsset(), soundAssetDictionary.getAll(), SoundAssetDictionary.NULL_SOUND_ASSET, soundAsset -> {
			weaponInfo.setWeaponHitSoundAsset(soundAsset);
			weaponInfo.setWeaponHitSoundAssetName(soundAsset.getName());
		}));
		this.row();

		this.add(WidgetBuilder.label("Miss Sound"));
		this.add(WidgetBuilder.select(weaponInfo.getWeaponMissSoundAsset(), soundAssetDictionary.getAll(), SoundAssetDictionary.NULL_SOUND_ASSET, soundAsset -> {
			weaponInfo.setWeaponMissSoundAsset(soundAsset);
			weaponInfo.setWeaponMissSoundAssetName(soundAsset.getName());
		}));
		this.row();

		this.add(WidgetBuilder.label("Animated sprite effect"));
		List<ParticleEffectType> animatedSpriteEffectTypes = particleEffectTypeDictionary.getAll().stream()
				.filter(type -> type.getAnimatedSpriteName() != null)
				.toList();
		this.add(WidgetBuilder.select(weaponInfo.getAnimatedEffectType(), animatedSpriteEffectTypes, NULL_PARTICLE_EFFECT, particleEffectType -> {
			if (particleEffectType.equals(NULL_PARTICLE_EFFECT)) {
				weaponInfo.setAnimatedSpriteEffectName(null);
				weaponInfo.setAnimatedEffectType(null);
			} else {
				weaponInfo.setAnimatedSpriteEffectName(particleEffectType.getName());
				weaponInfo.setAnimatedEffectType(particleEffectType);
			}
		}));
		this.row();
	}

}
