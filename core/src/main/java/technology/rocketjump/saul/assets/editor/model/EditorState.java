package technology.rocketjump.saul.assets.editor.model;

import com.google.inject.Singleton;

import java.nio.file.Path;

@Singleton
public class EditorState {

	private Path modDir;

	public Path getModDir() {
		return modDir;
	}

	public void setModDir(Path modDir) {
		this.modDir = modDir;
	}
}
