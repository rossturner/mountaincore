package technology.rocketjump.saul.assets.entities.furniture;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.rendering.RenderMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.saul.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES;
import static technology.rocketjump.saul.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES;
import static technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionaryProvider.addAnimatedSpriteArray;
import static technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionaryProvider.addSprite;

@Singleton
public class FurnitureEntityAssetDictionaryProvider implements Provider<FurnitureEntityAssetDictionary> {

	private final TextureAtlasRepository textureAtlasRepository;
	private final FurnitureTypeDictionary typeDictionary;
	private final FurnitureLayoutDictionary layoutDictionary;
	private final EntityAssetTypeDictionary assetTypeDictionary;

	private FurnitureEntityAssetDictionary instance;

	@Inject
	public FurnitureEntityAssetDictionaryProvider(TextureAtlasRepository textureAtlasRepository,
												  FurnitureTypeDictionary typeDictionary, FurnitureLayoutDictionary layoutDictionary,
												  EntityAssetTypeDictionary assetTypeDictionary) {
		this.textureAtlasRepository = textureAtlasRepository;
		this.typeDictionary = typeDictionary;
		this.layoutDictionary = layoutDictionary;
		this.assetTypeDictionary = assetTypeDictionary;
	}

	@Override
	public FurnitureEntityAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}

	private FurnitureEntityAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_ENTITIES);
		Path entityDefinitionsFile = Paths.get("assets/definitions/entityAssets/furnitureEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<FurnitureEntityAsset> assetList = objectMapper.readValue(Files.readString(entityDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FurnitureEntityAsset.class));

			for (FurnitureEntityAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					if (spriteDescriptor.getIsAnimated()) {
						addAnimatedSpriteArray(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
						addAnimatedSpriteArray(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
					} else {
						addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
						addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
					}
				}
			}

			return new FurnitureEntityAssetDictionary(assetList, typeDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}
	}

}
