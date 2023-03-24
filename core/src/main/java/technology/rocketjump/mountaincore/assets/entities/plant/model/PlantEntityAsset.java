package technology.rocketjump.mountaincore.assets.entities.plant.model;

import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;

import java.util.EnumMap;
import java.util.Map;

/**
 * This class represents a group of sprites in different orientations that represent a single asset e.g. a dwarf body
 */
public class PlantEntityAsset extends PlantEntityAssetDescriptor implements EntityAsset {

	private Map<EntityAssetOrientation, SpriteDescriptor> spriteDescriptors = new EnumMap<>(EntityAssetOrientation.class);

	@Override
	public Map<EntityAssetOrientation, SpriteDescriptor> getSpriteDescriptors() {
		return spriteDescriptors;
	}

	private Integer overrideRenderLayer;

	@Override
	public Integer getOverrideRenderLayer() {
		return overrideRenderLayer;
	}

	@Override
	public void setOverrideRenderLayer(Integer overrideRenderLayer) {
		this.overrideRenderLayer = overrideRenderLayer;
	}

}
