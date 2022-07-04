package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.components.EditorPane;
import technology.rocketjump.saul.assets.editor.components.TopLevelMenu;
import technology.rocketjump.saul.assets.editor.components.navigator.NavigatorPane;

@Singleton
public class AssetEditorUI  {

	private final Stage stage;

	private VisTable topLevelTable;
	private final VisTable viewArea;

	@Inject
	public AssetEditorUI(TopLevelMenu topLevelMenu, NavigatorPane navigatorPane, EditorPane editorPane) {

		stage = new Stage();
		topLevelTable = new VisTable();
		topLevelTable.setFillParent(true);

		topLevelTable.add(topLevelMenu.getTable()).expandX().fillX().colspan(3).row();

		topLevelTable.add(navigatorPane).top().left().expandY().fillY();

		viewArea = new VisTable();
		viewArea.setFillParent(true);
		topLevelTable.add(viewArea).expandX();

		topLevelTable.add(editorPane).top().right().expandY().fillY();

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
		ScreenViewport viewport = new ScreenViewport(new OrthographicCamera(width, height));
		viewport.setUnitsPerPixel(1);
		stage.setViewport(viewport);
		stage.getViewport().update(width, height, true);
	}
}
