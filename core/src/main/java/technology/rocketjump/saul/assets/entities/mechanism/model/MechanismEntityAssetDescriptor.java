package technology.rocketjump.saul.assets.entities.mechanism.model;

import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.saul.misc.Name;

public class MechanismEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private EntityAssetType type;
	private String mechanismTypeName;
	private Integer layoutId;

	public boolean matches(MechanismEntityAttributes entityAttributes) {
		if (mechanismTypeName != null && !mechanismTypeName.equals(entityAttributes.getMechanismType().getName())) {
			return false;
		}
		if (layoutId != null && entityAttributes.getPipeLayout() != null && !layoutId.equals(entityAttributes.getPipeLayout().getId())) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public String getMechanismTypeName() {
		return mechanismTypeName;
	}

	public void setMechanismTypeName(String MechanismTypeName) {
		this.mechanismTypeName = MechanismTypeName;
	}

	public Integer getLayoutId() {
		return layoutId;
	}

	public void setLayoutId(Integer layoutId) {
		this.layoutId = layoutId;
	}
}
