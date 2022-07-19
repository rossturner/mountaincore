package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.assets.editor.model.ColorPickerMessage;
import technology.rocketjump.saul.assets.editor.model.EditorAssetSelection;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.TopLevelMenu;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserContextMenu;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserPane;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserTreeMessage;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorContextMenu;
import technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorPane;
import technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorTreeMessage;
import technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorTreeValue;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.PropertyEditorPane;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.ViewEditorPane;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;

import static technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_ASSET_DESCRIPTOR;
import static technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_TYPE_DESCRIPTOR;

@Singleton
public class AssetEditorUI implements Telegraph {

	private final Stage stage;

	private final VisTable topLevelTable;
	private final VisTable viewArea;
	private final MessageDispatcher messageDispatcher;
	private final ViewEditorPane viewEditor;
	private final NavigatorContextMenu navigatorContextMenu;
	private final EntityBrowserContextMenu browserContextMenu;
	private final TopLevelMenu topLevelMenu;
	private final NavigatorPane navigatorPane;
	private final EntityBrowserPane entityBrowserPane;
	private final PropertyEditorPane propertyEditorPane;
	private final EditorStateProvider editorStateProvider;
	private final ColorPicker colorPicker;
	private ColorPickerMessage.ColorPickerCallback colorPickerCallback;

	//TODO: move
	private CreatureEntityFactory creatureEntityFactory;
	private RaceDictionary raceDictionary;
	private final CompleteAssetDictionary completeAssetDictionary;

	@Inject
	public AssetEditorUI(EntityBrowserContextMenu browserContextMenu, TopLevelMenu topLevelMenu, NavigatorPane navigatorPane,
						 EntityBrowserPane entityBrowserPane, PropertyEditorPane propertyEditorPane,
						 MessageDispatcher messageDispatcher, ViewEditorPane viewEditor, EditorStateProvider editorStateProvider,
						 CreatureEntityFactory creatureEntityFactory, RaceDictionary raceDictionary, CompleteAssetDictionary completeAssetDictionary) {
		this.browserContextMenu = browserContextMenu;
		this.topLevelMenu = topLevelMenu;
		this.navigatorPane = navigatorPane;
		this.entityBrowserPane = entityBrowserPane;
		this.propertyEditorPane = propertyEditorPane;
		this.messageDispatcher = messageDispatcher;
		this.viewEditor = viewEditor;
		this.editorStateProvider = editorStateProvider;
		this.creatureEntityFactory = creatureEntityFactory;
		this.raceDictionary = raceDictionary;
		this.completeAssetDictionary = completeAssetDictionary;

		stage = new Stage();
		topLevelTable = new VisTable();
		topLevelTable.setFillParent(true);

		viewArea = new VisTable();

		reload();

		stage.addListener(new InputListener() {
			@Override
			public boolean scrolled(InputEvent event, float x, float y, int amount) {
				// Setting scrollfocus when scrolling over a scrollpane as this is not default behaviour
				if (stage.getScrollFocus() == null) {
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
						return true;
					}
				}

				return false;
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				while (toActor != null) {
					if (toActor instanceof VisScrollPane) {
						break;
					}
					toActor = toActor.getParent();
				}

				if (stage.getScrollFocus() != null && toActor == null) {
					stage.setScrollFocus(null);
				}

			}

		});

		stage.addActor(topLevelTable);

		navigatorContextMenu = new NavigatorContextMenu(messageDispatcher);

		colorPicker = new ColorPicker(new ColorPickerAdapter() {
			@Override
			public void finished(Color color) {
				if (colorPickerCallback != null) {
					colorPickerCallback.colorPicked(color);
				}
			}
		});

		messageDispatcher.addListener(this, MessageType.EDITOR_NAVIGATOR_TREE_RIGHT_CLICK);
		messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_RIGHT_CLICK);
		messageDispatcher.addListener(this, MessageType.EDITOR_ENTITY_SELECTION);
		messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_SELECTION);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_COLOR_PICKER);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_CREATE_DIRECTORY_DIALOG);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_CREATE_ENTITY_DIALOG);
		messageDispatcher.addListener(this, MessageType.CAMERA_MOVED);
	}

	private void reload() {
		topLevelTable.clearChildren();
		viewEditor.reload();

		Actor leftPane;
		if (editorStateProvider.getState().getEntitySelection() == null) {
			leftPane = navigatorPane;
		} else {
			entityBrowserPane.reload();
			leftPane = entityBrowserPane;
		}

		// 3 Cols wide
		// Menu
		topLevelTable.add(topLevelMenu.getTable()).expandX().fillX().colspan(3).row();

		//Body
		VisTable viewSpace = new VisTable();
		viewSpace.add(viewArea).expand().row(); //View area has no components, so let it take as much as possible
		viewSpace.add(viewEditor).left().top().expandX().fill();

		topLevelTable.add(leftPane).top().left().expandY().fillY();
		topLevelTable.add(viewSpace).top().expand().fillY().fillX();
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
				Entity entity = null;
				if (selection != null) {
					entity = createEntity(selection);
				}
				propertyEditorPane.showControlsFor(null);
				editorStateProvider.getState().setCurrentEntity(entity);
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
			case MessageType.CAMERA_MOVED: {
				return true;
			}
			case MessageType.EDITOR_SHOW_CREATE_DIRECTORY_DIALOG: {
				final Path directory = FileUtils.getDirectory((Path) msg.extraInfo);

				VisLabel label = new VisLabel("Folder");
				VisValidatableTextField folderTextBox = new VisValidatableTextField();
				folderTextBox.addValidator(StringUtils::isNotBlank);

				OkCancelDialog dialog = new OkCancelDialog("Create subdirectory under " + directory) {
					@Override
					public void onOk() {
						FileUtils.createDirectory(directory, folderTextBox.getText());
						reload();
					}
				};
				dialog.add(label);
				dialog.add(folderTextBox);
				dialog.show(stage);
				stage.setKeyboardFocus(folderTextBox);
				folderTextBox.selectAll();
				return true;
			}
			case MessageType.EDITOR_SHOW_CREATE_ENTITY_DIALOG: {
				NavigatorTreeValue navigatorValue = (NavigatorTreeValue) msg.extraInfo;
				//TODO: refactor common widget
				//Todo: switch on entity type
				Function<String, Boolean> validRace = new Function<>() {
					@Override
					public Boolean apply(String input) {
						//TODO: define rules
						if (StringUtils.length(input) < 2) {
							return false;
						}
						Race existing = raceDictionary.getByName(input);
						if (existing != null) {
							return false;
						}
						return true;
					}
				};
				VisValidatableTextField typeDescriptorName = new VisValidatableTextField();
				typeDescriptorName.addValidator(validRace::apply);

				//TODO: think how to disable the form
				VisDialog dialog = new VisDialog("Create new " + navigatorValue.entityType) {
					@Override
					protected void result(Object result) {
						super.result(result);
						if (result instanceof Boolean doAdd) {
							if (doAdd) {
								String folderName = typeDescriptorName.getText().toLowerCase(Locale.ROOT);
								Path basePath = FileUtils.createDirectory(navigatorValue.path, folderName);
								//todo: switch behaviour based on entity type
								//TODO: maybe a nice function to setup required defaults
								String name = typeDescriptorName.getText();
								CreatureBodyShapeDescriptor bodyShape = new CreatureBodyShapeDescriptor();
								bodyShape.setValue(CreatureBodyShape.AVERAGE);
								Race newRace = new Race();
								newRace.setName(name);
								newRace.setI18nKey("RACE." + name.toUpperCase());
								newRace.setBodyStructureName("pawed-quadruped"); //TODO: Present user with options?
								newRace.setBodyShapes(List.of(bodyShape));
								raceDictionary.add(newRace);
								completeAssetDictionary.rebuild();

								EditorEntitySelection editorEntitySelection = new EditorEntitySelection();
								editorEntitySelection.setEntityType(navigatorValue.entityType);
								editorEntitySelection.setTypeName(name);
								editorEntitySelection.setBasePath(basePath.toString());
								messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, editorEntitySelection);
								messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, EntityBrowserValue.forTypeDescriptor(navigatorValue.entityType, basePath, newRace));
							}
						}
					}

				};

				dialog.getContentTable().add(new VisLabel(navigatorValue.entityType.typeDescriptorClass.getSimpleName()));
				dialog.getContentTable().add(typeDescriptorName);
				VisTextButton okButton = new VisTextButton("Ok") {
					@Override
					public Touchable getTouchable() {
						if (typeDescriptorName.isInputValid()) { //todo: brittle, nicer if this had a list of children to check
							setDisabled(false);
							return Touchable.enabled;
						} else {
							setDisabled(true);
							return Touchable.disabled;
						}
					}
				};
				dialog.button(okButton, true);
				dialog.button("Cancel", false);
//				dialog.key(Input.Keys.ENTER, true); //TODO: needs clever event handler to ignore event if form invalid
				dialog.key(Input.Keys.ESCAPE, false);

				dialog.show(stage);
				stage.setKeyboardFocus(typeDescriptorName);
				typeDescriptorName.selectAll();
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

	//TODO: consider moving me too
	private Entity createEntity(EditorEntitySelection entitySelection) {
		Random random = new Random();
		GameContext gameContext = new GameContext();
		gameContext.setRandom(new RandomXS128());
		switch (entitySelection.getEntityType()) {
			case CREATURE -> {
				Race race = raceDictionary.getByName(entitySelection.getTypeName());
				CreatureEntityAttributes attributes = new CreatureEntityAttributes(race, random.nextLong());
				Vector2 origin = new Vector2(0, 0f);
				return creatureEntityFactory.create(attributes, origin, origin, gameContext);
			}
			case PLANT -> {
			}
			case ITEM -> {
			}
			case FURNITURE -> {
			}
			case ONGOING_EFFECT -> {
			}
			case MECHANISM -> {
			}
		}
		return null;
	}
}
