package technology.rocketjump.saul.assets.editor.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class EditorStateProvider {

	public static final Path STATE_FILE = Paths.get("assets/editor-state.json");
	private static final String STATE_ENTITY_KEY = "currentEntity";

	private final EditorState stateInstance;

	@Inject
	public EditorStateProvider(SavedGameDependentDictionaries dictionaries) throws IOException, InvalidSaveException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		if (Files.exists(STATE_FILE)) {
			String fileText = Files.readString(STATE_FILE);
			try {
				this.stateInstance = JSON.parseObject(fileText, EditorState.class);
				JSONObject asJson = JSONObject.parseObject(fileText);
				loadEntity(dictionaries, asJson);
			} catch (JSONException jse) {
				Logger.error(jse, "Something went wrong parsing the file text " + fileText);
				throw jse;
			}
		} else {
			this.stateInstance = new EditorState();
			this.stateInstance.setModDir("mods/base");
		}
	}

	public EditorState getState() {
		return stateInstance;
	}

	public void stateChanged() {
		try {
			JSONObject asJsonObject = (JSONObject) JSON.toJSON(stateInstance);

			Entity currentEntity = stateInstance.getCurrentEntity();
			if (currentEntity != null) {
				SavedGameStateHolder savedGameStateHolder = new SavedGameStateHolder();
				currentEntity.writeTo(savedGameStateHolder);
				long currentEntityId = currentEntity.getId();
				for (int i = 0; i < savedGameStateHolder.entitiesJson.size(); i++) {
					JSONObject entityJson = savedGameStateHolder.entitiesJson.getJSONObject(i);
					if (currentEntityId == entityJson.getLong("id")) {
						asJsonObject.put(STATE_ENTITY_KEY, entityJson);
					}
				}
			}

			String outputText = JSON.toJSONString(asJsonObject, true);
			Files.writeString(STATE_FILE, outputText);
		} catch (IOException e) {
			Logger.error("Could not store editor state as JSON", e);
		}
	}


	private void loadEntity(SavedGameDependentDictionaries dictionaries, JSONObject asJson) {
		try {
			this.stateInstance.setCurrentEntity(null);
			if (asJson.containsKey(STATE_ENTITY_KEY)) {
				JSONObject entityJson = asJson.getJSONObject(STATE_ENTITY_KEY);
				SavedGameStateHolder savedGameStateHolder = new SavedGameStateHolder();
				savedGameStateHolder.entitiesJson.add(entityJson);

				Entity persistable = Entity.class.getDeclaredConstructor().newInstance();
				persistable.readFrom(entityJson, savedGameStateHolder, dictionaries);

				this.stateInstance.setCurrentEntity(persistable);
			}
		} catch (Exception ex) {
			Logger.warn(ex, "Failed to load entity, will continue");
		}
	}

}
