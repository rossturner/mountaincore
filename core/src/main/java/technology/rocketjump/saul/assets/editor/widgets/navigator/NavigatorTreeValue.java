package technology.rocketjump.saul.assets.editor.widgets.navigator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.misc.Name;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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
		Path path = directory.resolve(entityType.descriptorFilename);
		String fileText = Files.readString(path);
		try {
			JSONObject descriptorJson = JSON.parseObject(fileText);

			Field nameField = Arrays.stream(entityType.descriptorClass.getDeclaredFields())
					.filter(field -> field.isAnnotationPresent(Name.class))
					.findFirst()
					.orElseThrow();

			String name = descriptorJson.getString(nameField.getName());
			return new NavigatorTreeValue(TreeValueType.ENTITY_DIR,
					entityType,
					directory,
					name);

		} catch (JSONException jsonException) {
			Logger.error("Error parsing json at file " + path + " due to " + jsonException.getMessage(), jsonException);
			throw jsonException;
		}

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
