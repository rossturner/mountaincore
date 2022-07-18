package technology.rocketjump.saul.assets.editor.widgets.entitybrowser;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.fasterxml.jackson.databind.ObjectMapper;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.entities.model.EntityType;

import java.nio.file.Path;

public class EntityBrowserValue extends Tree.Node {

	private static ObjectMapper objectMapper = new ObjectMapper();

	public final TreeValueType treeValueType;
	public final EntityType entityType;
	public final Path path;
	public final String label;

	private Object typeDescriptor;
	private EntityAsset entityAsset;

	public static EntityBrowserValue forTypeDescriptor(EntityType entityType, Path baseDir, Object typeDescriptorInstance) {
		Path typeDescriptorFile = baseDir.resolve(entityType.descriptorFilename);
		EntityBrowserValue value = new EntityBrowserValue(TreeValueType.ENTITY_TYPE_DESCRIPTOR, entityType, typeDescriptorFile, entityType.descriptorFilename);
		value.setTypeDescriptor(typeDescriptorInstance);
		return value;
	}

	public static EntityBrowserValue forSubDirectory(EntityType entityType, Path dirPath) {
		return new EntityBrowserValue(TreeValueType.SUBDIR, entityType, dirPath, dirPath.getFileName().toString());
	}

	public static EntityBrowserValue forAsset(EntityType entityType, Path descriptorsFile, EntityAsset entityAsset) {
		EntityBrowserValue value = new EntityBrowserValue(TreeValueType.ENTITY_ASSET_DESCRIPTOR, entityType, descriptorsFile, entityAsset.getUniqueName());
		value.setEntityAsset(entityAsset);
		return value;
	}

	public EntityBrowserValue(TreeValueType type, EntityType entityType, Path path, String label) {
		this.treeValueType = type;
		this.entityType = entityType;
		this.path = path;
		this.label = label;
	}

	public Object getTypeDescriptor() {
		return typeDescriptor;
	}

	public void setTypeDescriptor(Object typeDescriptor) {
		this.typeDescriptor = typeDescriptor;
	}

	public enum TreeValueType {

		ENTITY_TYPE_DESCRIPTOR,
		SUBDIR,
		ENTITY_ASSET_DESCRIPTOR

	}

	public TreeValueType getTreeValueType() {
		return treeValueType;
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public Path getPath() {
		return path;
	}

	public String getLabel() {
		return label;
	}

	public EntityAsset getEntityAsset() {
		return entityAsset;
	}

	public void setEntityAsset(EntityAsset entityAsset) {
		this.entityAsset = entityAsset;
	}


}
