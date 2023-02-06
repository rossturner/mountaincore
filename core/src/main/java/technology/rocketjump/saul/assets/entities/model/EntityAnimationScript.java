package technology.rocketjump.saul.assets.entities.model;

import technology.rocketjump.saul.misc.Name;

import java.util.HashMap;
import java.util.Map;

public class EntityAnimationScript {
	@Name
	private String name;
	private Map<EntityAssetOrientation, AnimationScript> orientations = new HashMap<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<EntityAssetOrientation, AnimationScript> getOrientations() {
		return orientations;
	}

	public void setOrientations(Map<EntityAssetOrientation, AnimationScript> orientations) {
		this.orientations = orientations;
	}
}
