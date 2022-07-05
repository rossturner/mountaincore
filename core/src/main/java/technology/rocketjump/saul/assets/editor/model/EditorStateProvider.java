package technology.rocketjump.saul.assets.editor.model;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public class EditorStateProvider {

	private static final String FILENAME = "assets/editor-state.json";

	private EditorState stateInstance;

	@Inject
	public EditorStateProvider() throws IOException {
		Path stateFile = new File(FILENAME).toPath();
		if (Files.exists(stateFile)) {
			this.stateInstance = JSON.parseObject(Files.readString(stateFile), EditorState.class);
		} else {
			this.stateInstance = new EditorState();
			this.stateInstance.setModDir("mods/base");
		}
	}


	public EditorState getState() {
		return stateInstance;
	}

	public void stateChanged() {
		Gson gson = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
		String outputText = gson.toJson(stateInstance);
		try {
			Files.writeString(new File(FILENAME).toPath(), outputText);
		} catch (IOException e) {
			Logger.error("Could not store editor state as JSON", e);
		}
	}


}
