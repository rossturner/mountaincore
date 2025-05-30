package technology.rocketjump.mountaincore.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.particles.custom_libgdx.AnimatedSpriteEffect;
import technology.rocketjump.mountaincore.particles.custom_libgdx.DefensePoolBarEffect;
import technology.rocketjump.mountaincore.particles.custom_libgdx.ParticleEffect;
import technology.rocketjump.mountaincore.particles.custom_libgdx.ProgressBarEffect;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectInstance;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class CustomEffectFactory {

	public static final String PROGRESS_BAR_EFFECT_TYPE_NAME = "Progress bar";
	public static final String DEFENSE_POOL_EFFECT_TYPE_NAME = "Defense pool bar";
	private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	private final Sprite progressBarOuterSprite;
	private final Sprite progressBarInnerSprite;
	private final Sprite defensePoolShieldSprite;
	private final Sprite defensePoolForegroundSprite;
	private final Sprite defensePoolBackgroundSprite;
	private final ParticleEffectType progressBarEffectType;
	private final ParticleEffectType defensePoolEffectType;
	private final TextureAtlas diffuseEntitiesAtlas;

	@Inject
	public CustomEffectFactory(TextureAtlasRepository textureAtlasRepository, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;

		// TODO more generic loading of custom effects
		progressBarEffectType = particleEffectTypeDictionary.getByName(PROGRESS_BAR_EFFECT_TYPE_NAME);
		defensePoolEffectType = particleEffectTypeDictionary.getByName(DEFENSE_POOL_EFFECT_TYPE_NAME);

		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		diffuseEntitiesAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES);

		progressBarOuterSprite = diffuseTextureAtlas.createSprite("job_progress_bg");
		progressBarInnerSprite = diffuseTextureAtlas.createSprite("job_progress_fg");

		defensePoolShieldSprite = diffuseTextureAtlas.createSprite("Shield_Bar_Icon");
		defensePoolForegroundSprite = diffuseTextureAtlas.createSprite("Shield_Bar_Foreground_Trimmed");
		defensePoolBackgroundSprite = diffuseTextureAtlas.createSprite("Shield_Bar_Background");
	}

	/**
	 * Note that this is assuming animated sprite effects apply to all 8 directions/orientations
	 */
	public ParticleEffectInstance createAnimatedSpriteEffect(ParticleEffectType type, Entity parentEntity, Optional<Color> optionalMaterialColor) {
		Array<Sprite> sprites = diffuseEntitiesAtlas.createSprites(type.getAnimatedSpriteName());
		if (sprites.isEmpty()) {
			Logger.error("Can not find sprites with name " + type.getAnimatedSpriteName());
			return null;
		} else {
			AnimatedSpriteEffect animatedSpriteEffect = new AnimatedSpriteEffect(sprites, type.getOverrideDuration(), type.getScale(), type.getIsLooping());
			if (type.isUsesTargetMaterialAsTintColor() && optionalMaterialColor.isPresent()) {
				animatedSpriteEffect.setTint(optionalMaterialColor.get());
			}


			EntityAssetOrientation parentOrientation = EntityAssetOrientation.fromFacingTo8Directions(parentEntity.getLocationComponent().getFacing());
			if (type.getUsingParentOrientation() != null) {
				EntityAssetOrientation effectDefaultOrientation = type.getUsingParentOrientation();

				if (!parentOrientation.equals(effectDefaultOrientation)) {
					animatedSpriteEffect.adjustForParentOrientation(effectDefaultOrientation, parentOrientation);
				}
			}

			ParticleEffectInstance effectInstance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), type, animatedSpriteEffect, parentEntity);

			Vector2 offset = parentOrientation.toVector2().cpy().scl(type.getDistanceFromParentEntityOrientation());
			effectInstance.setOffsetFromWorldPosition(offset);
			return effectInstance;
		}
	}

	public ParticleEffectInstance createProgressBarEffect(Entity parentEntity) {
		ParticleEffect progressBarEffect = new ProgressBarEffect(progressBarInnerSprite, progressBarOuterSprite);
		ParticleEffectInstance particleEffectInstance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), progressBarEffectType, progressBarEffect, parentEntity);
		particleEffectInstance.setOffsetFromWorldPosition(progressBarEffectType.getOffsetFromParentEntity());

		return particleEffectInstance;
	}


	public ParticleEffectInstance createDefenseBarEffect(Entity parentEntity) {
		ParticleEffect defensePoolBarEffect = new DefensePoolBarEffect(defensePoolShieldSprite, defensePoolForegroundSprite, defensePoolBackgroundSprite);
		ParticleEffectInstance particleEffectInstance = new ParticleEffectInstance(SequentialIdGenerator.nextId(), defensePoolEffectType, defensePoolBarEffect, parentEntity);
		particleEffectInstance.setOffsetFromWorldPosition(defensePoolEffectType.getOffsetFromParentEntity());
		return particleEffectInstance;
	}
}
