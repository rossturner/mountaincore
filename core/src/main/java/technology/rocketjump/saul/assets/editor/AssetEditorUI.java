package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;
import technology.rocketjump.saul.assets.editor.components.TopLevelMenu;
import technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserContextMenu;
import technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserPane;
import technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserTreeMessage;
import technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.components.navigator.NavigatorContextMenu;
import technology.rocketjump.saul.assets.editor.components.navigator.NavigatorPane;
import technology.rocketjump.saul.assets.editor.components.navigator.NavigatorTreeMessage;
import technology.rocketjump.saul.assets.editor.components.propertyeditor.PropertyEditorPane;
import technology.rocketjump.saul.assets.editor.model.ColorPickerMessage;
import technology.rocketjump.saul.assets.editor.model.EditorAssetSelection;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;

import static technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_ASSET_DESCRIPTOR;
import static technology.rocketjump.saul.assets.editor.components.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_TYPE_DESCRIPTOR;

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
	private final ColorPicker colorPicker;
	private ColorPickerMessage.ColorPickerCallback colorPickerCallback;

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

		colorPicker = new ColorPicker(new ColorPickerAdapter() {
			@Override
			public void finished (Color color) {
				if (colorPickerCallback != null) {
					colorPickerCallback.colorPicked(color);
				}
			}
		});

		stage.addListener(new InputListener() {
			@Override
			public boolean scrolled(InputEvent event, float x, float y, int amount) {
				// Setting scrollfocus when scrolling over a scrollpane as this is not default behaviour
				Actor hitActor = stage.hit(x, y, false);
				while (hitActor != null) {
					if (hitActor instanceof VisScrollPane) {
						break;
					}
					hitActor = hitActor.getParent();
				}
				if (hitActor != null) {
					// Over a parent scrollpane
					stage.setScrollFocus(hitActor);
				}

				return super.scrolled(event, x, y, amount);
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				// Removing scroll focus when exiting actor with that focus to avoid weird behaviour
				if (stage.getScrollFocus() != null && toActor != null && toActor.equals(stage.getScrollFocus())) {
					stage.setScrollFocus(null);
				}
				super.exit(event, x, y, pointer, toActor);
			}

		});

		messageDispatcher.addListener(this, MessageType.EDITOR_NAVIGATOR_TREE_RIGHT_CLICK);
		messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_RIGHT_CLICK);
		messageDispatcher.addListener(this, MessageType.EDITOR_ENTITY_SELECTION);
		messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_SELECTION);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_COLOR_PICKER);
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
				EntityBrowserValue value = (EntityBrowserValue) msg.extraInfo;

				if (value != null) {
					EditorAssetSelection selection = new EditorAssetSelection();
					selection.setBasePath(value.path.toString());
					selection.setUniqueName(value.label);
					editorStateProvider.getState().setAssetSelection(selection);
					editorStateProvider.stateChanged();
					if (value.treeValueType.equals(ENTITY_TYPE_DESCRIPTOR)) {
						propertyEditorPane.showControlsFor(value.getTypeDescriptor());
					} else if (value.treeValueType.equals(ENTITY_ASSET_DESCRIPTOR)) {
						propertyEditorPane.showControlsFor(value.getEntityAsset());
					}
				} else {
					editorStateProvider.getState().setAssetSelection(null);
					editorStateProvider.stateChanged();
					propertyEditorPane.showControlsFor(null);
				}
				return true;
			}
			case MessageType.EDITOR_SHOW_COLOR_PICKER: {
				ColorPickerMessage message = (ColorPickerMessage) msg.extraInfo;
				this.colorPickerCallback = message.callback;
				colorPicker.setColor(HexColors.get(message.initialHexCode));
				getStage().addActor(colorPicker.fadeIn());
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
