package technology.rocketjump.mountaincore.assets.entities.model;

import java.util.HashSet;
import java.util.Set;

public class EntityChildAssetDescriptor {

	private EntityAssetType type;
	private String specificAssetName; // Rather than a type
	private StorableVector2 offsetPixels;
	private Integer overrideRenderLayer;
	private Set<String> inheritAnimations = new HashSet<>();

	public static final EntityChildAssetDescriptor UNSPECIFIED_CHILD_ASSET = new EntityChildAssetDescriptor();
	static {
		UNSPECIFIED_CHILD_ASSET.setType(new EntityAssetType(EntityAssetType.UNSPECIFIED));
		UNSPECIFIED_CHILD_ASSET.setOffsetPixels(new StorableVector2());
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public StorableVector2 getOffsetPixels() {
		if (offsetPixels == null) {
			this.offsetPixels = new StorableVector2();
		}
		return offsetPixels;
	}

	public void setOffsetPixels(StorableVector2 offsetPixels) {
		this.offsetPixels = offsetPixels;
	}

	public String getSpecificAssetName() {
		return specificAssetName;
	}

	public void setSpecificAssetName(String specificAssetName) {
		this.specificAssetName = specificAssetName;
	}

	public Integer getOverrideRenderLayer() {
		return overrideRenderLayer;
	}

	public void setOverrideRenderLayer(Integer overrideRenderLayer) {
		this.overrideRenderLayer = overrideRenderLayer;
	}
	// may need rotation from parentGoal

	public Set<String> getInheritAnimations() {
		return inheritAnimations;
	}

	public void setInheritAnimations(Set<String> inheritAnimations) {
		this.inheritAnimations = inheritAnimations;
	}
}
