package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.saul.jobs.SkillDictionary.NULL_PROFESSION;

@Singleton
public class ImageButtonFactory {

	private final Skin managementSkin;
	private final TextureAtlas textureAtlas;
	private final NinePatch buttonNinePatch;

	private final MessageDispatcher messageDispatcher;

	private Map<String, ImageButton> byIconName = new HashMap<>();
	private final Map<Long, ImageButton> entityButtonsByEntityId = new HashMap<>();
	private final EntityRenderer entityRenderer;

	@Inject
	public ImageButtonFactory(TextureAtlasRepository textureAtlasRepository, SkillDictionary skillDictionary,
	                          MessageDispatcher messageDispatcher, EntityRenderer entityRenderer, GuiSkinRepository guiSkinRepository) {
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.textureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		this.messageDispatcher = messageDispatcher;
		this.entityRenderer = entityRenderer;
		this.buttonNinePatch = textureAtlas.createPatch("button");

		for (Skill profession : skillDictionary.getAllProfessions()) {
			profession.setImageButton(getOrCreate(profession.getIcon()));
		}
		NULL_PROFESSION.setImageButton(getOrCreate(NULL_PROFESSION.getIcon()));
	}

	public ImageButton getOrCreate(String iconName) {
		return getOrCreate(iconName, false);
	}

	public ImageButton getOrCreate(String iconName, boolean halfSize) {

		return byIconName.computeIfAbsent(iconName, (i) -> {
			//TODO: this is backwards compatibility for remainder of game, as drawables moving to skin
			if (managementSkin.has(iconName, Drawable.class) || managementSkin.has(iconName, TextureRegion.class)) {
				return new ImageButton(managementSkin.getDrawable(iconName), buttonNinePatch, halfSize);
			}

			Sprite iconSprite = this.textureAtlas.createSprite(iconName);
			if (iconSprite == null) {
				throw new RuntimeException("Could not find UI sprite with name " + iconName);
			}
			return new ImageButton(iconSprite, buttonNinePatch, halfSize);
		});
	}

	public ImageButton getOrCreate(Entity entity) {
		return entityButtonsByEntityId.computeIfAbsent(entity.getId(), a -> new ImageButton(new EntityDrawable(entity, entityRenderer, true, messageDispatcher), buttonNinePatch, false));
	}

}
