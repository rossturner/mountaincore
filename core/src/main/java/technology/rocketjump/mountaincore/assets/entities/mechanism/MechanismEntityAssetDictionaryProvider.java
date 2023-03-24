package technology.rocketjump.mountaincore.assets.entities.mechanism;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionaryProvider;
import technology.rocketjump.mountaincore.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.mountaincore.rendering.RenderMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class MechanismEntityAssetDictionaryProvider implements Provider<MechanismEntityAssetDictionary> {

	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final TextureAtlasRepository textureAtlasRepository;
	private final MechanismTypeDictionary mechanismTypeDictionary;
	private MechanismEntityAssetDictionary instance;

	@Inject
	public MechanismEntityAssetDictionaryProvider(EntityAssetTypeDictionary entityAssetTypeDictionary,
												  TextureAtlasRepository textureAtlasRepository,
												  MechanismTypeDictionary mechanismTypeDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.textureAtlasRepository = textureAtlasRepository;
		this.mechanismTypeDictionary = mechanismTypeDictionary;
	}

	@Override
	public MechanismEntityAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}

	public MechanismEntityAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES);
		FileHandle entityDefinitionsFile = Gdx.files.internal("assets/definitions/entityAssets/mechanismEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<MechanismEntityAsset> assetList = objectMapper.readValue(entityDefinitionsFile.readString(),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, MechanismEntityAsset.class));

			for (MechanismEntityAsset asset : assetList) {
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

			return new MechanismEntityAssetDictionary(assetList, mechanismTypeDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}
	}

}
