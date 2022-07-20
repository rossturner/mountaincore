package technology.rocketjump.saul.assets.editor.model;

import com.alibaba.fastjson.annotation.JSONField;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.rendering.RenderMode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class EditorState {

	private String modDir;
	private Set<String> expandedNavigatorNodes = new HashSet<>();
	private EditorEntitySelection entitySelection;
	private EditorAssetSelection assetSelection;
	private RenderMode renderMode = RenderMode.DIFFUSE;
	private float spritePadding = 1;
	@JSONField(serialize = false, deserialize = false)
	private Entity currentEntity;

	public String getModDir() {
		return modDir;
	}

	@JSONField(serialize = false)
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

	public Entity getCurrentEntity() {
		return currentEntity;
	}

	public void setCurrentEntity(Entity currentEntity) {
		this.currentEntity = currentEntity;
	}

	public float getSpritePadding() {
		return spritePadding;
	}

	public void setSpritePadding(float spritePadding) {
		this.spritePadding = spritePadding;
	}
}
