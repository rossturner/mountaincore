package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
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

	@Inject
	public AssetEditorUI() {

		stage = new Stage();
		topLevelTable = new VisTable();
		topLevelTable.setFillParent(true);

		MenuBar topLevelMenu = new MenuBar();
		topLevelMenu.setMenuListener(new MenuBar.MenuBarListener() {
			@Override
			public void menuOpened (Menu menu) {
				System.out.println("Opened menu: " + menu.getTitle());
			}

			@Override
			public void menuClosed (Menu menu) {
				System.out.println("Closed menu: " + menu.getTitle());
			}
		});

		Menu fileMenu = new Menu("File");
		fileMenu.addItem(new MenuItem("TODO #1"));
		topLevelMenu.addMenu(fileMenu);

		topLevelTable.add(topLevelMenu.getTable()).expandX().fillX().colspan(3).row();


		navigatorTree = new VisTree();
		for (EntityType entityType : EntityType.values()) {
			TreeNode entityTypeNode = new TreeNode(new VisLabel(entityType.name()));
			navigatorTree.add(entityTypeNode);
		}
		VisScrollPane navigatorScrollPane = new VisScrollPane(navigatorTree);

		VisTable navigatorWindowTable = new VisTable();
		navigatorWindowTable.setDebug(true);
		navigatorWindowTable.background("window-bg");
		navigatorWindowTable.add(new VisLabel("Navigator")).left().row();
		navigatorWindowTable.add(navigatorScrollPane).top().row();
		navigatorWindowTable.add(new VisTable()).expandY();

		topLevelTable.add(navigatorWindowTable).top().left().expandY().fillY();

		viewArea = new VisTable();
		viewArea.setFillParent(true);
		topLevelTable.add(viewArea).expandX();


		VisTable editorTable = new VisTable();
		editorTable.add(new VisLabel("TODO: Put stuff here")).pad(5);
		VisScrollPane editorScrollPane = new VisScrollPane(editorTable);

		VisTable editorWindowTable = new VisTable();
		editorWindowTable.setDebug(true);
		editorWindowTable.background("window-bg");
		editorWindowTable.add(new VisLabel("Property Editor")).left().row();
		editorWindowTable.add(editorScrollPane).top().row();
		editorWindowTable.add(new VisTable()).expandY();

		topLevelTable.add(editorWindowTable).top().right().expandY().fillY();

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
