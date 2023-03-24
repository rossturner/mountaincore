package technology.rocketjump.mountaincore.assets.entities.item;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionaryProvider;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.rendering.RenderMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ItemEntityAssetDictionaryProvider implements Provider<ItemEntityAssetDictionary> {

	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final TextureAtlasRepository textureAtlasRepository;
	private final ItemTypeDictionary itemTypeDictionary;
	private ItemEntityAssetDictionary instance;

	@Inject
	public ItemEntityAssetDictionaryProvider(EntityAssetTypeDictionary entityAssetTypeDictionary,
											 TextureAtlasRepository textureAtlasRepository, ItemTypeDictionary itemTypeDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.textureAtlasRepository = textureAtlasRepository;
		this.itemTypeDictionary = itemTypeDictionary;
	}

	@Override
	public ItemEntityAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}


	public ItemEntityAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES);
		Path entityDefinitionsFile = Paths.get("assets/definitions/entityAssets/itemEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<ItemEntityAsset> assetList = objectMapper.readValue(Files.readString(entityDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, ItemEntityAsset.class));

			for (ItemEntityAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					if (spriteDescriptor.getFilename() != null) {
						CreatureEntityAssetDictionaryProvider.addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
						CreatureEntityAssetDictionaryProvider.addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
					}
				}
			}

			return new ItemEntityAssetDictionary(assetList, entityAssetTypeDictionary, itemTypeDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}
	}

}
