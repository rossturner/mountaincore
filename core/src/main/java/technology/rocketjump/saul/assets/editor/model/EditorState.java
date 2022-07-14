package technology.rocketjump.saul.assets.editor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.rendering.RenderMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EditorState {

	private String modDir;
	private Set<String> expandedNavigatorNodes = new HashSet<>();
	private EditorEntitySelection entitySelection;
	private EditorAssetSelection assetSelection;
	private RenderMode renderMode = RenderMode.DIFFUSE;

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

	public EditorEntitySelection getEntitySelection() {
		return entitySelection;
	}

	public void setEntitySelection(EditorEntitySelection entitySelection) {
		this.entitySelection = entitySelection;
	}

	public EditorAssetSelection getAssetSelection() {
		return assetSelection;
	}

	public void setAssetSelection(EditorAssetSelection assetSelection) {
		this.assetSelection = assetSelection;
	}

	public RenderMode getRenderMode() {
		return renderMode;
	}

	public void setRenderMode(RenderMode renderMode) {
		this.renderMode = renderMode;
	}
}
