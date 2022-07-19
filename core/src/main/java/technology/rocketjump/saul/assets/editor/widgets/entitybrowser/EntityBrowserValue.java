package technology.rocketjump.saul.assets.editor.widgets.entitybrowser;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.entities.model.EntityType;

import java.nio.file.Path;

public class EntityBrowserValue extends Tree.Node {

	public final TreeValueType treeValueType;
	public final EntityType entityType;
	public final Path path;
	public final String label;
	private final Object typeDescriptor;

	private EntityAsset entityAsset;

	public static EntityBrowserValue forTypeDescriptor(EntityType entityType, Path baseDir, Object typeDescriptorInstance) {
		Path typeDescriptorFile = baseDir.resolve(entityType.descriptorFilename);
		TreeValueType treeValueType = TreeValueType.ENTITY_TYPE_DESCRIPTOR;
		return new EntityBrowserValue(treeValueType, entityType, typeDescriptorFile, entityType.descriptorFilename, typeDescriptorInstance);
	}

	public static EntityBrowserValue forSubDirectory(EntityType entityType, Path dirPath, Object typeDescriptor) {
		TreeValueType treeValueType = TreeValueType.SUBDIR;
		return new EntityBrowserValue(treeValueType, entityType, dirPath, dirPath.getFileName().toString(), typeDescriptor);
	}

	public static EntityBrowserValue forAsset(EntityType entityType, Path descriptorsFile, EntityAsset entityAsset, Object typeDescriptor) {
		TreeValueType treeValueType = TreeValueType.ENTITY_ASSET_DESCRIPTOR;
		EntityBrowserValue value = new EntityBrowserValue(treeValueType, entityType, descriptorsFile, entityAsset.getUniqueName(), typeDescriptor);
		value.setEntityAsset(entityAsset);
		return value;
	}

	private EntityBrowserValue(TreeValueType type, EntityType entityType, Path path, String label, Object typeDescriptor) {
		this.treeValueType = type;
		this.entityType = entityType;
		this.path = path;
		this.label = label;
		this.typeDescriptor = typeDescriptor;
	}

	public Object getTypeDescriptor() {
		return typeDescriptor;
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
