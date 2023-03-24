package technology.rocketjump.mountaincore.assets.entities.vehicle;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;
import technology.rocketjump.mountaincore.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.mountaincore.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.mountaincore.rendering.RenderMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionaryProvider.addAnimatedSpriteArray;
import static technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionaryProvider.addSprite;

@Singleton
public class VehicleEntityAssetDictionaryProvider implements Provider<VehicleEntityAssetDictionary> {

	private final TextureAtlasRepository textureAtlasRepository;
	private final VehicleTypeDictionary typeDictionary;
	private final EntityAssetTypeDictionary assetTypeDictionary;

	private VehicleEntityAssetDictionary instance;

	@Inject
	public VehicleEntityAssetDictionaryProvider(TextureAtlasRepository textureAtlasRepository,
												VehicleTypeDictionary typeDictionary,
												EntityAssetTypeDictionary assetTypeDictionary) {
		this.textureAtlasRepository = textureAtlasRepository;
		this.typeDictionary = typeDictionary;
		this.assetTypeDictionary = assetTypeDictionary;
	}

	@Override
	public VehicleEntityAssetDictionary get() {
		if (instance == null) {
			instance = create();
		}
		return instance;
	}

	private VehicleEntityAssetDictionary create() {
		TextureAtlas diffuseTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.DIFFUSE_ENTITIES);
		TextureAtlas normalTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.NORMAL_ENTITIES);
		Path entityDefinitionsFile = Paths.get("assets/definitions/entityAssets/vehicleEntityAssets.json");
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			List<VehicleEntityAsset> assetList = objectMapper.readValue(Files.readString(entityDefinitionsFile),
					objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, VehicleEntityAsset.class));

			for (VehicleEntityAsset asset : assetList) {
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

			return new VehicleEntityAssetDictionary(assetList, typeDictionary);
		} catch (IOException e) {
			// TODO better exception handling
			throw new RuntimeException(e);
		}
	}

}
