package technology.rocketjump.saul.assets.editor.widgets.propertyeditor;

import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import java.util.List;

import static technology.rocketjump.saul.assets.editor.factory.ItemUIFactory.ammoTypeSelect;
import static technology.rocketjump.saul.audio.model.SoundAssetDictionary.NULL_SOUND_ASSET;

public class WeaponInfoWidget extends VisTable {

	private static final ParticleEffectType NULL_PARTICLE_EFFECT = new ParticleEffectType();
	static {
		NULL_PARTICLE_EFFECT.setName("-none-");
	}

	public WeaponInfoWidget(WeaponInfo weaponInfo, SoundAssetDictionary soundAssetDictionary, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.columnDefaults(0).uniformX().left();
		this.columnDefaults(1).fillX().left();

		this.add(WidgetBuilder.label("Modified By Strength"));
		this.add(WidgetBuilder.toggle(weaponInfo.isModifiedByStrength(), weaponInfo::setModifiedByStrength));
		this.row();

		this.add(WidgetBuilder.label("Min Damage"));
		this.add(WidgetBuilder.intSpinner(weaponInfo.getMinDamage(), 0, Integer.MAX_VALUE, weaponInfo::setMinDamage));
		this.row();

		this.add(WidgetBuilder.label("Max Damage"));
		this.add(WidgetBuilder.intSpinner(weaponInfo.getMaxDamage(), 0, Integer.MAX_VALUE, weaponInfo::setMaxDamage));
		this.row();

		this.add(WidgetBuilder.label("Damage Type"));
		this.add(WidgetBuilder.select(weaponInfo.getDamageType(), CombatDamageType.values(), null, weaponInfo::setDamageType));
		this.row();

		this.add(WidgetBuilder.label("Range"));
		this.add(WidgetBuilder.floatSpinner(weaponInfo.getRange(), 0, Float.MAX_VALUE, weaponInfo::setRange));
		this.row();

		this.add(WidgetBuilder.label("Requires Ammo"));
		this.add(ammoTypeSelect(weaponInfo.getRequiresAmmoType(), weaponInfo::setRequiresAmmoType));
		this.row();

		this.add(WidgetBuilder.label("Fire Sound"));
		this.add(WidgetBuilder.select(weaponInfo.getFireWeaponSoundAsset(), soundAssetDictionary.getAll(), NULL_SOUND_ASSET, soundAsset -> {
			weaponInfo.setFireWeaponSoundAsset(soundAsset);
			weaponInfo.setFireWeaponSoundAssetName(soundAsset.getName());
		}));
		this.row();

		this.add(WidgetBuilder.label("Hit Sound"));
		this.add(WidgetBuilder.select(weaponInfo.getWeaponHitSoundAsset(), soundAssetDictionary.getAll(), NULL_SOUND_ASSET, soundAsset -> {
			weaponInfo.setWeaponHitSoundAsset(soundAsset);
			weaponInfo.setWeaponHitSoundAssetName(soundAsset.getName());
		}));
		this.row();

		this.add(WidgetBuilder.label("Miss Sound"));
		this.add(WidgetBuilder.select(weaponInfo.getWeaponMissSoundAsset(), soundAssetDictionary.getAll(), NULL_SOUND_ASSET, soundAsset -> {
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
