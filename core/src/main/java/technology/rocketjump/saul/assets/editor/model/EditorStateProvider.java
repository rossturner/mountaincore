package technology.rocketjump.saul.assets.editor.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class EditorStateProvider {

	private static final String FILENAME = "assets/editor-state.json";
	private static final String STATE_ENTITY_KEY = "currentEntity";

	private final EditorState stateInstance;

	@Inject
	public EditorStateProvider(SavedGameDependentDictionaries dictionaries) throws IOException, InvalidSaveException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		Path stateFile = new File(FILENAME).toPath();
		if (Files.exists(stateFile)) {
			String fileText = Files.readString(stateFile);
			this.stateInstance = JSON.parseObject(fileText, EditorState.class);
			JSONObject asJson = JSONObject.parseObject(fileText);
			loadEntity(dictionaries, asJson);

		} else {
			this.stateInstance = new EditorState();
			this.stateInstance.setModDir("mods/base");
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

	public EditorState getState() {
		return stateInstance;
	}

	public void stateChanged() {
		try {
			JSONObject asJsonObject = (JSONObject) JSON.toJSON(stateInstance);

			if (stateInstance.getCurrentEntity() != null) {
				SavedGameStateHolder savedGameStateHolder = new SavedGameStateHolder();
				stateInstance.getCurrentEntity().writeTo(savedGameStateHolder);

				JSONObject serializedEntity = savedGameStateHolder.entitiesJson.getJSONObject(0);
				asJsonObject.put(STATE_ENTITY_KEY, serializedEntity);
			}

			String outputText = JSON.toJSONString(asJsonObject, true);
			Files.writeString(new File(FILENAME).toPath(), outputText);
		} catch (IOException e) {
			Logger.error("Could not store editor state as JSON", e);
		}
	}


}
