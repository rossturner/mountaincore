package technology.rocketjump.mountaincore.assets.entities.furniture;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionaryProvider;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.rendering.RenderMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES);
		Path entityDefinitionsFile = Paths.get("assets/definitions/entityAssets/furnitureEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<FurnitureEntityAsset> assetList = objectMapper.readValue(Files.readString(entityDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, FurnitureEntityAsset.class));

			for (FurnitureEntityAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					if (spriteDescriptor.getIsAnimated()) {
						CreatureEntityAssetDictionaryProvider.addAnimatedSpriteArray(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
						CreatureEntityAssetDictionaryProvider.addAnimatedSpriteArray(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
					} else {
						CreatureEntityAssetDictionaryProvider.addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
						CreatureEntityAssetDictionaryProvider.addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
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
