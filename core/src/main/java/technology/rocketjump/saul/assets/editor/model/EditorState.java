package technology.rocketjump.saul.assets.editor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EditorState {

	private String modDir;
	private Set<String> expandedNavigatorNodes = new HashSet<>();

	public String getModDir() {
		return modDir;
	}

	@JsonIgnore
	public Path getModDirPath() {
		return Paths.get(modDir);
	}

	public void setModDir(String modDir) {
		this.modDir = modDir;
	}

	public Set<String> getExpandedNavigatorNodes() {
		return expandedNavigatorNodes;
	}

	public void setExpandedNavigatorNodes(Set<String> expandedNavigatorNodes) {
		this.expandedNavigatorNodes = expandedNavigatorNodes;
	}
}
