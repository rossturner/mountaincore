package technology.rocketjump.mountaincore.assets.entities.wallcap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.WallTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionaryProvider;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.mountaincore.assets.entities.wallcap.model.WallCapAsset;
import technology.rocketjump.mountaincore.rendering.RenderMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class WallCapAssetDictionaryProvider implements Provider<WallCapAssetDictionary> {

	private final TextureAtlasRepository textureAtlasRepository;
	private final WallTypeDictionary wallTypeDictionary;
	private WallCapAssetDictionary instance;

	@Inject
	public WallCapAssetDictionaryProvider(TextureAtlasRepository textureAtlasRepository, WallTypeDictionary wallTypeDictionary) {
		this.textureAtlasRepository = textureAtlasRepository;
		this.wallTypeDictionary = wallTypeDictionary;
	}

	@Override
	public WallCapAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}

	public WallCapAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES);
		FileHandle assetDefinitionsFile = Gdx.files.internal("assets/definitions/entityAssets/wallCapAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<WallCapAsset> assetList = objectMapper.readValue(assetDefinitionsFile.readString(),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, WallCapAsset.class));

			for (WallCapAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					CreatureEntityAssetDictionaryProvider.addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
					CreatureEntityAssetDictionaryProvider.addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
				}
			}

			return new WallCapAssetDictionary(assetList, wallTypeDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}

	}

}
