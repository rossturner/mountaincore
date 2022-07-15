package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;
import technology.rocketjump.saul.assets.editor.model.ColorPickerMessage;
import technology.rocketjump.saul.assets.editor.model.EditorAssetSelection;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.TopLevelMenu;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserContextMenu;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserPane;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserTreeMessage;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorContextMenu;
import technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorPane;
import technology.rocketjump.saul.assets.editor.widgets.navigator.NavigatorTreeMessage;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.PropertyEditorPane;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.List;
import java.util.Random;

import static technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_ASSET_DESCRIPTOR;
import static technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_TYPE_DESCRIPTOR;

@Singleton
public class AssetEditorUI implements Telegraph {

	private final Stage stage;

	private final VisTable topLevelTable;
	private final VisTable viewEditor;
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

	private CreatureEntityFactory creatureEntityFactory;
	//TODO: move
	private EntityAssetUpdater entityAssetUpdater;
	private RaceDictionary raceDictionary;

	@Inject
	public AssetEditorUI(EntityBrowserContextMenu browserContextMenu, TopLevelMenu topLevelMenu, NavigatorPane navigatorPane,
						 EntityBrowserPane entityBrowserPane, PropertyEditorPane propertyEditorPane,
						 MessageDispatcher messageDispatcher, EditorStateProvider editorStateProvider,
						 CreatureEntityFactory creatureEntityFactory, EntityAssetUpdater entityAssetUpdater, RaceDictionary raceDictionary) {
		this.browserContextMenu = browserContextMenu;
		this.topLevelMenu = topLevelMenu;
		this.navigatorPane = navigatorPane;
		this.entityBrowserPane = entityBrowserPane;
		this.propertyEditorPane = propertyEditorPane;
		this.editorStateProvider = editorStateProvider;
		this.creatureEntityFactory = creatureEntityFactory;
		this.entityAssetUpdater = entityAssetUpdater;
		this.raceDictionary = raceDictionary;

		stage = new Stage();
		topLevelTable = new VisTable();
		topLevelTable.setFillParent(true);

		//TODO: Refactor into own class
		viewEditor = new VisTable();
		viewEditor.debug();
		viewEditor.setBackground("window-bg");
		viewEditor.add(new VisLabel("View Editor")).expandX().left().row();

		//Render row
		VisTable renderModeRow = new VisTable();
		renderModeRow.add(new VisLabel("Render Mode"));
		ButtonGroup<VisRadioButton> renderModeButtonGroup = new ButtonGroup<>();
		for (RenderMode renderMode : RenderMode.values()) {
			VisRadioButton radioButton = new VisRadioButton(renderMode.name());
			renderModeButtonGroup.add(radioButton);
			radioButton.setChecked(renderMode == editorStateProvider.getState().getRenderMode());
			radioButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (radioButton.isChecked()) {
						editorStateProvider.getState().setRenderMode(renderMode);
						editorStateProvider.stateChanged();
					}
				}
			});
			renderModeRow.add(radioButton);
		}
		viewEditor.add(renderModeRow).left().row();
		//TODO: do properly with data and right components

		//Creature Attribute Row
		if (editorStateProvider.getState().getCurrentEntity() != null) {
			if (editorStateProvider.getState().getCurrentEntity().getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureAttributes) {

		//		entityAssetUpdater.updateEntityAssets(currentEntity);//todo: do we be lazy and do this every render loop?

				VisTable creatureAttributeRow = new VisTable();
				creatureAttributeRow.add(
					WidgetBuilder.selectField("Gender:", creatureAttributes.getGender(), creatureAttributes.getRace().getGenders().keySet(), null, gender -> {
							creatureAttributes.setGender(gender);
							entityAssetUpdater.updateEntityAssets(editorStateProvider.getState().getCurrentEntity());
							editorStateProvider.stateChanged();
						})
				);
				List<CreatureBodyShape> bodyShapes = creatureAttributes.getRace().getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();
				creatureAttributeRow.add(
						WidgetBuilder.selectField("Body Shape:", creatureAttributes.getBodyShape(), bodyShapes, null, bodyShape -> {
							creatureAttributes.setBodyShape(bodyShape);
							entityAssetUpdater.updateEntityAssets(editorStateProvider.getState().getCurrentEntity());
							editorStateProvider.stateChanged();
						})
				);
				creatureAttributeRow.add(new VisLabel("Consciousness:"));
				creatureAttributeRow.add(new VisSelectBox<>());
				creatureAttributeRow.add(new VisLabel("Profession:"));
				creatureAttributeRow.add(new VisSelectBox<>());
				viewEditor.add(creatureAttributeRow).left().row();

		//		HorizontalFlowGroup assetTypeFlowGroup = new HorizontalFlowGroup(5); //TODO: not 100% is right separate flow groups
		//		assetTypeFlowGroup.addActor(new VisLabel("Hair:"));
		//		assetTypeFlowGroup.addActor(new VisSelectBox<>());
		//		assetTypeFlowGroup.addActor(new VisLabel("Eyebrows:"));
		//		assetTypeFlowGroup.addActor(new VisSelectBox<>());
		//		assetTypeFlowGroup.addActor(new VisLabel("Beard:"));
		//		assetTypeFlowGroup.addActor(new VisSelectBox<>());
		//		assetTypeFlowGroup.addActor(new VisLabel("Clothing:"));
		//		assetTypeFlowGroup.addActor(new VisSelectBox<>());
		//		viewEditor.add(assetTypeFlowGroup).expandX().fillX();


			}
		}


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

		navigatorContextMenu = new NavigatorContextMenu();

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
		messageDispatcher.addListener(this, MessageType.CAMERA_MOVED);
	}

	private void reload() {
		topLevelTable.clearChildren();

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

	private Entity createEntity(EditorEntitySelection entitySelection) {
		Random random = new Random();
		GameContext gameContext = new GameContext();
		gameContext.setRandom(new RandomXS128());
		switch (entitySelection.getEntityType()) {
			case CREATURE -> {
				Race race = raceDictionary.getByName(entitySelection.getTypeName());
				CreatureEntityAttributes attributes = new CreatureEntityAttributes(race, random.nextLong()); //TODO: persistable too?
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
