package technology.rocketjump.saul.assets.editor.widgets.navigator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.model.EntityType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NavigatorTreeValue {

	public final TreeValueType treeValueType;
	public final EntityType entityType;
	public final Path path;
	public final String label;

	public static NavigatorTreeValue forEntityType(EntityType entityType, Path modDir) {
		String label = entityType.name().toLowerCase();
		return new NavigatorTreeValue(TreeValueType.ENTITY_TYPE, entityType, modDir.resolve("entities").resolve(label), label);
	}

	public static NavigatorTreeValue forSubDir(EntityType entityType, Path subDir) {
		return new NavigatorTreeValue(TreeValueType.SUBDIR,
				entityType,
				subDir,
				subDir.getFileName().toString());
	}

	public static NavigatorTreeValue forEntityDir(EntityType entityType, Path directory) throws IOException {
		JSONObject descriptorJson = JSON.parseObject(Files.readString(directory.resolve(entityType.descriptorFilename)));
		String name = descriptorJson.getString("name");

		return new NavigatorTreeValue(TreeValueType.ENTITY_DIR,
				entityType,
				directory,
				name);
	}

	public NavigatorTreeValue(TreeValueType type, EntityType entityType, Path path, String label) {
		this.treeValueType = type;
		this.entityType = entityType;
		this.path = path;
		this.label = label;
	}

	public enum TreeValueType {

		ENTITY_TYPE,
		SUBDIR,
		ENTITY_DIR

	}

}
