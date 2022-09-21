package technology.rocketjump.saul.entities.model.physical.creature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceGenderDescriptor {

	private float weighting;
	private Map<EntityAssetType, Float> hideAssetTypes = new HashMap<>();

	public float getWeighting() {
		return weighting;
	}

	public void setWeighting(float weighting) {
		this.weighting = weighting;
	}

	public Map<EntityAssetType, Float> getHideAssetTypes() {
		return hideAssetTypes;
	}

	public void setHideAssetTypes(Map<EntityAssetType, Float> hideAssetTypes) {
		this.hideAssetTypes = hideAssetTypes;
	}
}
