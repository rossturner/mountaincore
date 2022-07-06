package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.components.TopLevelMenu;
import technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserContextMenu;
import technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserPane;
import technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserTreeMessage;
import technology.rocketjump.saul.assets.editor.components.navigator.NavigatorContextMenu;
import technology.rocketjump.saul.assets.editor.components.navigator.NavigatorPane;
import technology.rocketjump.saul.assets.editor.components.navigator.NavigatorTreeMessage;
import technology.rocketjump.saul.assets.editor.components.propertyeditor.PropertyEditorPane;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.messaging.MessageType;

@Singleton
public class AssetEditorUI implements Telegraph {

	private final Stage stage;

	private final VisTable topLevelTable;
	private final VisTable viewArea;
	private final NavigatorContextMenu navigatorContextMenu;
	private final EntityBrowserContextMenu browserContextMenu;
	private final TopLevelMenu topLevelMenu;
	private final NavigatorPane navigatorPane;
	private final EntityBrowserPane entityBrowserPane;
	private final PropertyEditorPane propertyEditorPane;
	private final EditorStateProvider editorStateProvider;

	@Inject
	public AssetEditorUI(EntityBrowserContextMenu browserContextMenu, TopLevelMenu topLevelMenu, NavigatorPane navigatorPane,
						 EntityBrowserPane entityBrowserPane, PropertyEditorPane propertyEditorPane,
						 MessageDispatcher messageDispatcher, EditorStateProvider editorStateProvider) {
		this.browserContextMenu = browserContextMenu;
		this.topLevelMenu = topLevelMenu;
		this.navigatorPane = navigatorPane;
		this.entityBrowserPane = entityBrowserPane;
		this.propertyEditorPane = propertyEditorPane;
		this.editorStateProvider = editorStateProvider;

		stage = new Stage();
		topLevelTable = new VisTable();
		topLevelTable.setFillParent(true);


		viewArea = new VisTable();
		viewArea.setFillParent(true);

		reload();

		stage.addActor(topLevelTable);

		navigatorContextMenu = new NavigatorContextMenu();

		messageDispatcher.addListener(this, MessageType.EDITOR_NAVIGATOR_TREE_RIGHT_CLICK);
		messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_RIGHT_CLICK);
		messageDispatcher.addListener(this, MessageType.EDITOR_ENTITY_SELECTION);
		messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_SELECTION);
	}

	private void reload() {
		topLevelTable.clearChildren();

		topLevelTable.add(topLevelMenu.getTable()).expandX().fillX().colspan(3).row();


		if (editorStateProvider.getState().getEntitySelection() == null) {
			topLevelTable.add(navigatorPane).top().left().expandY().fillY();
		} else {
			entityBrowserPane.reload();
			topLevelTable.add(entityBrowserPane).top().left().expandY().fillY();
		}

		topLevelTable.add(viewArea).expandX();

		topLevelTable.add(propertyEditorPane).top().right().expandY().fillY();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.EDITOR_NAVIGATOR_TREE_RIGHT_CLICK: {
				NavigatorTreeMessage message = (NavigatorTreeMessage) msg.extraInfo;
				navigatorContextMenu.setContext(message.value);
				navigatorContextMenu.showMenu(stage, message.actor);
				return true;
			}
			case MessageType.EDITOR_BROWSER_TREE_RIGHT_CLICK: {
				EntityBrowserTreeMessage message = (EntityBrowserTreeMessage) msg.extraInfo;
				browserContextMenu.setContext(message.value);
				browserContextMenu.showMenu(stage, message.actor);
				return true;
			}
			case MessageType.EDITOR_ENTITY_SELECTION: {
				EditorEntitySelection selection = (EditorEntitySelection) msg.extraInfo;
				propertyEditorPane.showControlsFor(null);
				editorStateProvider.getState().setEntitySelection(selection);
				editorStateProvider.stateChanged();
				reload();
				return true;
			}
			case MessageType.EDITOR_BROWSER_TREE_SELECTION: {
				Object selected = msg.extraInfo;
				propertyEditorPane.showControlsFor(selected);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
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
