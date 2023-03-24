package technology.rocketjump.mountaincore.assets.editor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.entities.model.EntityType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EditorEntitySelection {

	private EntityType entityType;
	private String typeName;
	private String basePath;

	public EntityType getEntityType() {
		return entityType;
	}

	public void setEntityType(EntityType entityType) {
		this.entityType = entityType;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
}
