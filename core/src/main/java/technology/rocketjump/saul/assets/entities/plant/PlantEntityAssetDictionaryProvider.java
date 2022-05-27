package technology.rocketjump.saul.assets.entities.plant;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.rendering.RenderMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.saul.assets.TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES;
import static technology.rocketjump.saul.assets.TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES;
import static technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionaryProvider.addSprite;

@Singleton
public class PlantEntityAssetDictionaryProvider implements Provider<PlantEntityAssetDictionary> {

	private final EntityAssetTypeDictionary entityAssetTypeDictionary;
	private final TextureAtlasRepository textureAtlasRepository;
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private PlantEntityAssetDictionary instance;

	@Inject
	public PlantEntityAssetDictionaryProvider(EntityAssetTypeDictionary entityAssetTypeDictionary,
											  TextureAtlasRepository textureAtlasRepository, PlantSpeciesDictionary plantSpeciesDictionary) {
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.textureAtlasRepository = textureAtlasRepository;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
	}

	@Override
	public PlantEntityAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}

	public PlantEntityAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(NORMAL_ENTITIES);
		FileHandle entityDefinitionsFile = Gdx.files.internal("assets/definitions/entityAssets/plantEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<PlantEntityAsset> assetList = objectMapper.readValue(entityDefinitionsFile.readString(),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, PlantEntityAsset.class));

			for (PlantEntityAsset asset : assetList) {
				for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
					addSprite(spriteDescriptor, diffuseTextureAtlas, RenderMode.DIFFUSE);
					addSprite(spriteDescriptor, normalTextureAtlas, RenderMode.NORMALS);
				}
			}

			return new PlantEntityAssetDictionary(assetList, entityAssetTypeDictionary, plantSpeciesDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}

	}

}
