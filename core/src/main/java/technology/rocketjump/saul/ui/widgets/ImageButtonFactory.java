package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.saul.jobs.ProfessionDictionary.NULL_PROFESSION;

@Singleton
public class ImageButtonFactory {

	private final TextureAtlas textureAtlas;
	private final NinePatch buttonNinePatch;

	private Map<String, ImageButton> byIconName = new HashMap<>();
	private final Map<Long, ImageButton> entityButtonsByEntityId = new HashMap<>();
	private final Map<Long, ImageButton> ghostButtonsByEntityId = new HashMap<>();
	private final EntityRenderer entityRenderer;

	@Inject
	public ImageButtonFactory(TextureAtlasRepository textureAtlasRepository, ProfessionDictionary professionDictionary, EntityRenderer entityRenderer) {
		this.textureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		this.entityRenderer = entityRenderer;
		this.buttonNinePatch = textureAtlas.createPatch("button");

		for (Profession profession : professionDictionary.getAll()) {
			profession.setImageButton(getOrCreate(profession.getIcon()));
		}
		NULL_PROFESSION.setImageButton(getOrCreate(NULL_PROFESSION.getIcon()));
	}

	public ImageButton getOrCreate(String iconName) {
		return getOrCreate(iconName, false);
	}

	public ImageButton getOrCreate(String iconName, boolean halfSize) {

		return byIconName.computeIfAbsent(iconName, (i) -> {
			Sprite iconSprite = this.textureAtlas.createSprite(iconName);
			if (iconSprite == null) {
				throw new RuntimeException("Could not find UI sprite with name " + iconName);
			}
			return new ImageButton(iconSprite, buttonNinePatch, halfSize);
		});
	}

	public ImageButton getOrCreate(Entity entity) {
		return entityButtonsByEntityId.computeIfAbsent(entity.getId(), a -> new ImageButton(new EntityDrawable(entity, entityRenderer), buttonNinePatch, false));
	}

	public ImageButton getOrCreateGhostButton(Entity entity) {
		return ghostButtonsByEntityId.computeIfAbsent(entity.getId(), a -> {
			EntityDrawable entityDrawable = new EntityDrawable(entity, entityRenderer);
			entityDrawable.setOverrideColor(HexColors.get("#D4534C88"));
			return new ImageButton(entityDrawable, buttonNinePatch, false);
		});
	}

}
