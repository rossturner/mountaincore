package technology.rocketjump.mountaincore.assets.entities.mechanism.model;

import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.assets.entities.model.SpriteDescriptor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MechanismEntityAsset extends MechanismEntityAssetDescriptor implements EntityAsset {

	private Map<EntityAssetOrientation, SpriteDescriptor> spriteDescriptors = new EnumMap<>(EntityAssetOrientation.class);

	@Override
	public Map<EntityAssetOrientation, SpriteDescriptor> getSpriteDescriptors() {
		return spriteDescriptors;
	}

	@Override
	public Map<String, List<String>> getTags() {
		return null; // FIXME #109 Add tags to Mechanisms
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
