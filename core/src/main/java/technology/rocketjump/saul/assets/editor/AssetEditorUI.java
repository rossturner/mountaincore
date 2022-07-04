package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.*;
import technology.rocketjump.saul.assets.editor.components.TreeNode;
import technology.rocketjump.saul.entities.model.EntityType;

@Singleton
public class AssetEditorUI  {

	private final Stage stage;

	private VisTable topLevelTable;

	private final VisTree navigatorTree;

	private final VisTable viewArea;
	private final VisWindow navigatorWindow;

	@Inject
	public AssetEditorUI() {

		stage = new Stage();
		topLevelTable = new VisTable();
		topLevelTable.setFillParent(true);


		navigatorWindow = new VisWindow("Navigator");
		navigatorTree = new VisTree();
		for (EntityType entityType : EntityType.values()) {
			TreeNode entityTypeNode = new TreeNode(new VisLabel(entityType.name()));
			navigatorTree.add(entityTypeNode);
		}
		VisScrollPane navigatorScrollPane = new VisScrollPane(navigatorTree);

		VisTable navigatorWindowTable = new VisTable();
		navigatorWindowTable.setDebug(true);
		navigatorWindowTable.add(navigatorScrollPane).top().row();
		navigatorWindowTable.add(new VisTable()).expandY();
		navigatorWindow.add(navigatorWindowTable).expandY().fillY();

		topLevelTable.add(navigatorWindow).top().left().expandY().fillY();

		viewArea = new VisTable();
		viewArea.setFillParent(true);
		topLevelTable.add(viewArea).expandX();

		stage.addActor(topLevelTable);
	}

	public Stage getStage() {
		return stage;
	}

	public void render() {
		stage.act();
		stage.draw();
	}

	public void onResize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}
}
