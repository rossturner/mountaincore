package technology.rocketjump.mountaincore.assets.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.layout.GridGroup;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.editor.factory.UIFactory;
import technology.rocketjump.mountaincore.assets.editor.model.*;
import technology.rocketjump.mountaincore.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.mountaincore.assets.editor.widgets.TextBoxConventionListener;
import technology.rocketjump.mountaincore.assets.editor.widgets.TopLevelMenu;
import technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserContextMenu;
import technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserPane;
import technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserTreeMessage;
import technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.mountaincore.assets.editor.widgets.navigator.NavigatorContextMenu;
import technology.rocketjump.mountaincore.assets.editor.widgets.navigator.NavigatorPane;
import technology.rocketjump.mountaincore.assets.editor.widgets.navigator.NavigatorTreeMessage;
import technology.rocketjump.mountaincore.assets.editor.widgets.navigator.NavigatorTreeValue;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.PropertyEditorPane;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.SpriteDescriptorsPane;
import technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor.ViewEditorPane;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.modding.ModParser;
import technology.rocketjump.mountaincore.modding.model.ModInfo;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;
import technology.rocketjump.mountaincore.persistence.FileUtils;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.rendering.entities.AnimationStudio;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_ASSET_DESCRIPTOR;
import static technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserValue.TreeValueType.ENTITY_TYPE_DESCRIPTOR;

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
	private final SpriteDescriptorsPane spriteDescriptorsPane;
	private final EditorStateProvider editorStateProvider;
	private final ColorPicker colorPicker;
	private final Map<EntityType, UIFactory> uiFactories;
	private ColorPickerMessage.ColorPickerCallback colorPickerCallback;
	private UIFactory currentUiFactory;
	private final SpriteCropperPipeline spriteCropperPipeline;
	private final AnimationStudio animationStudio;
	private final ModParser modParser;
	//TODO: this class definitely getting big

	@Inject
	public AssetEditorUI(EntityBrowserContextMenu browserContextMenu, TopLevelMenu topLevelMenu, NavigatorPane navigatorPane,
						 EntityBrowserPane entityBrowserPane, PropertyEditorPane propertyEditorPane,
						 MessageDispatcher messageDispatcher, ViewEditorPane viewEditor, AnimationStudio animationStudio,
						 SpriteDescriptorsPane spriteDescriptorsPane, EditorStateProvider editorStateProvider,
						 Map<EntityType, UIFactory> uiFactories, SpriteCropperPipeline spriteCropperPipeline, ModParser modParser) {
		this.browserContextMenu = browserContextMenu;
		this.topLevelMenu = topLevelMenu;
		this.navigatorPane = navigatorPane;
		this.entityBrowserPane = entityBrowserPane;
		this.propertyEditorPane = propertyEditorPane;
		this.messageDispatcher = messageDispatcher;
		this.viewEditor = viewEditor;
		this.spriteDescriptorsPane = spriteDescriptorsPane;
		this.editorStateProvider = editorStateProvider;
		this.uiFactories = uiFactories;
		this.spriteCropperPipeline = spriteCropperPipeline;
		this.animationStudio = animationStudio;
		this.modParser = modParser;

		stage = new Stage();
		topLevelTable = new VisTable();
		topLevelTable.setFillParent(true);

		viewArea = new VisTable();

		reload();

		stage.addListener(new InputListener() {


			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				// Setting scrollfocus when scrolling over a scrollpane as this is not default behaviour
				if (stage.getScrollFocus() == null) {
					Actor hitActor = stage.hit(x, y, false);
					while (hitActor != null) {
						if (hitActor instanceof ScrollPane) {
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
					if (toActor instanceof ScrollPane) {
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
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_CREATE_ASSET_DIALOG);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_IMPORT_FILE_DIALOG);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_CROP_SPRITES_DIALOG);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_ICON_SELECTION_DIALOG);
		messageDispatcher.addListener(this, MessageType.EDITOR_SHOW_CREATE_MOD_DIALOG);
		messageDispatcher.addListener(this, MessageType.EDITOR_OPEN_MOD);
		messageDispatcher.addListener(this, MessageType.CAMERA_MOVED);
	}

	private void reload() {
		topLevelTable.clearChildren();
		viewEditor.reload();

		Actor leftPane;
		if (editorStateProvider.getState().getEntitySelection() == null) {
			leftPane = navigatorPane;
			navigatorPane.reloadTree();
		} else {
			currentUiFactory = uiFactories.get(editorStateProvider.getState().getEntitySelection().getEntityType());
			entityBrowserPane.reload();
			leftPane = entityBrowserPane;
		}

		// 3 Cols wide
		// Menu
		topLevelTable.add(topLevelMenu.getTable()).expandX().fillX().colspan(3).row();

		topLevelTable.add(leftPane).top().left().expand().fillY();
		topLevelTable.add(viewArea).top().minWidth(Gdx.graphics.getWidth() / 3.0f);
		topLevelTable.add(propertyEditorPane).top().right().expand().fillY();
		topLevelTable.row();
		topLevelTable.add(viewEditor).colspan(3).center().fillX();
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
				animationStudio.dispose(); //clear caches to make key frame jumping easier
				EditorEntitySelection selection = (EditorEntitySelection) msg.extraInfo;
				Entity entity = null;
				if (selection != null) {
					currentUiFactory = uiFactories.get(selection.getEntityType());
					entity = currentUiFactory.createEntityForRendering(selection.getTypeName());
				}
				propertyEditorPane.clear();
				editorStateProvider.getState().setCurrentEntity(entity);
				editorStateProvider.getState().setEntitySelection(selection);
				editorStateProvider.stateChanged();
				reload();
				return true;
			}
			case MessageType.EDITOR_BROWSER_TREE_SELECTION: {
				EntityBrowserValue value = (EntityBrowserValue) msg.extraInfo;

				if (value != null) {
					if (editorStateProvider.getState().isAutosave()) {
						entityBrowserPane.saveChanges();
					}
					EditorAssetSelection selection = new EditorAssetSelection();
					selection.setBasePath(value.path.toString());
					selection.setUniqueName(value.label);
					editorStateProvider.getState().setAssetSelection(selection);
					editorStateProvider.stateChanged();
					if (value.treeValueType.equals(ENTITY_TYPE_DESCRIPTOR)) {
						propertyEditorPane.setControls(currentUiFactory.getEntityPropertyControls(value.getTypeDescriptor(), value.path));
					} else if (value.treeValueType.equals(ENTITY_ASSET_DESCRIPTOR)) {
						//TODO: fix vertical layout
						EntityAsset entityAsset = value.getEntityAsset();
						VisTable assetControls = currentUiFactory.getAssetPropertyControls(entityAsset);
						assetControls.add(spriteDescriptorsPane).expandX().fillX().colspan(2).left().row(); //assumes 2 cols?
						List<EntityAssetOrientation> applicableOrientations = currentUiFactory.getApplicableOrientations(entityAsset);
						List<ColoringLayer> applicableColoringLayers = currentUiFactory.getApplicableColoringLayers();
						spriteDescriptorsPane.showSpriteDescriptorControls(entityAsset, currentUiFactory.getEntityType(), applicableOrientations, applicableColoringLayers);
						propertyEditorPane.setControls(assetControls);
					}
				} else {
					editorStateProvider.getState().setAssetSelection(null);
					editorStateProvider.stateChanged();
					propertyEditorPane.clear();
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

				VisValidatableTextField folderTextBox = new VisValidatableTextField();
				folderTextBox.addValidator(StringUtils::isNotBlank);
				folderTextBox.addListener(new TextBoxConventionListener(original -> original.toLowerCase(Locale.ROOT).replaceAll("\\s", "_")));
				OkCancelDialog dialog = new OkCancelDialog("Create subdirectory under " + directory) {
					@Override
					public void onOk() {
						FileUtils.createDirectory(directory, folderTextBox.getText());
						reload();
					}
				};
				dialog.add(new VisLabel("Folder"));
				dialog.add(folderTextBox);
				dialog.show(stage);
				return true;
			}
			case MessageType.EDITOR_OPEN_MOD: {
				final FileHandle file = (FileHandle) msg.extraInfo;
				Path modDir = FileUtils.getDirectory(file.file().toPath());

				try {
					ParsedMod parsedMod = modParser.parseMod(modDir);
					if (parsedMod.getInfo().isBaseMod() && !GlobalSettings.DEV_MODE) {
						VisDialog baseModDialog = new VisDialog("Error") {
							@Override
							protected void result(Object object) {
								super.result(object);
								editorStateProvider.getState().changeToMod(null);
								editorStateProvider.stateChanged();
								messageDispatcher.dispatchMessage(MessageType.EDITOR_RELOAD);
							}
						};
						baseModDialog.text("Editing the base mod is disabled");
						baseModDialog.button("Ok");
						baseModDialog.show(stage);
					} else {
						editorStateProvider.getState().changeToMod(modDir.toAbsolutePath().toString());
						editorStateProvider.stateChanged();
						messageDispatcher.dispatchMessage(MessageType.EDITOR_RELOAD);
					}
				} catch (IOException e) {
					Logger.error("unable to parse log", e);
					VisDialog errorDialog = new VisDialog("Error");
					errorDialog.text("Unable to open mod: " + e.getMessage());
					errorDialog.button("Ok");
					errorDialog.show(stage);
				}
				return true;
			}
			case MessageType.EDITOR_SHOW_CREATE_MOD_DIALOG: {
				final Path directory = Paths.get("mods/").toAbsolutePath();
				VisValidatableTextField modNameTextField = new VisValidatableTextField();
				modNameTextField.addValidator(StringUtils::isNotBlank);
				modNameTextField.addListener(new TextBoxConventionListener(original -> original.toLowerCase(Locale.ROOT).replaceAll("\\s", "_")));

				VisTextArea descriptionTextField = new VisTextArea();
				VisTextField homepageTextField = new VisTextField();

				OkCancelDialog dialog = new OkCancelDialog("New Mod") {
					@Override
					public void onOk() {
						String name = modNameTextField.getText();
						Path modDir = FileUtils.createDirectory(directory, name);
						ModInfo modInfo = new ModInfo();
						modInfo.setName(name);
						modInfo.setGameVersion(GlobalSettings.VERSION.toString());
						modInfo.setDescription(descriptionTextField.getText());
						modInfo.setHomepageUrl(homepageTextField.getText());
						modInfo.setVersion("1.0.0");
						try {
							modParser.write(modDir, modInfo);
							messageDispatcher.dispatchMessage(MessageType.EDITOR_OPEN_MOD, new FileHandle(modDir.toFile()));
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
				};
				dialog.add(new VisLabel("Name"));
				dialog.add(modNameTextField);
				dialog.row();
				dialog.add(new VisLabel(directory.toString())).colspan(2);
				dialog.row();
				dialog.add(new VisLabel("Homepage Url"));
				dialog.add(homepageTextField);
				dialog.row();
				dialog.add(new VisLabel("Description"));
				dialog.add(descriptionTextField);
				dialog.row();
				dialog.show(stage);
				return true;
			}
			case MessageType.EDITOR_SHOW_CREATE_ENTITY_DIALOG: {
				NavigatorTreeValue navigatorValue = (NavigatorTreeValue) msg.extraInfo;
				Path path = navigatorValue.path;
				OkCancelDialog dialog = uiFactories.get(navigatorValue.entityType).createEntityDialog(path);
				dialog.show(stage);
				return true;
			}
			case MessageType.EDITOR_SHOW_CREATE_ASSET_DIALOG: {
				ShowCreateAssetDialogMessage message = (ShowCreateAssetDialogMessage) msg.extraInfo;
				OkCancelDialog dialog = currentUiFactory.createAssetDialog(message);
				dialog.show(stage);
				return true;
			}
			case MessageType.EDITOR_SHOW_ICON_SELECTION_DIALOG: {
				ShowIconSelectDialogMessage message = (ShowIconSelectDialogMessage) msg.extraInfo;

				List<Path> icons = FileUtils.findFilesByFilename(editorStateProvider.getState().getModDirPath().resolve("icons"),
						Pattern.compile(".*\\.png"));

				var selection = new Object() {
					private VisTable selectedImage = new VisTable();
					private Path selectedIcon = null;
					private final Drawable defaultBgSkin = VisUI.getSkin().getDrawable("window-bg");
					private final Drawable selectedBgSkin = VisUI.getSkin().getDrawable("list-selection");
					void select(VisTable image, Path path) {
						selectedImage.setBackground(defaultBgSkin);

						selectedIcon = path;
						selectedImage = image;
						selectedImage.setBackground(selectedBgSkin);
					}
				};

				GridGroup group = new GridGroup(64, 4);
				for (Path iconPath : icons) {
					Texture texture = new Texture(new FileHandle(iconPath.toFile()));
					Image image = new Image(texture);
					VisTable imageContainer = new VisTable();
					imageContainer.add(image);
					group.addActor(imageContainer);
					imageContainer.addListener(new ClickListener() {
						@Override
						public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
							selection.select(imageContainer, iconPath);
							return true;
						}
					});

					if (message.initialIconName() != null && iconPath.toString().contains(message.initialIconName())) {
						selection.select(imageContainer, iconPath);
					}
				}

				VisScrollPane scrollPane = new VisScrollPane(group);
				scrollPane.setScrollbarsVisible(true);
				scrollPane.setScrollingDisabled(true, false);

				OkCancelDialog dialog = new OkCancelDialog("Select an icon") {
					@Override
					public void onOk() {
						if (selection.selectedIcon != null) {
							message.callback().accept(FilenameUtils.removeExtension(selection.selectedIcon.getFileName().toString()));
						}
					}
				};

				dialog.add(scrollPane).prefWidth(64 * 9).prefHeight(64 * 3);
				dialog.show(stage);
				return true;
			}
			case MessageType.EDITOR_SHOW_CROP_SPRITES_DIALOG: {
				Path path = (Path) msg.extraInfo;
				Path directoryToProcess = FileUtils.getDirectory(path);

				OkCancelDialog dialog = new OkCancelDialog("Crop sprites in " + directoryToProcess) {
					@Override
					public void onOk() {
						entityBrowserPane.saveChanges();
						spriteCropperPipeline.process(directoryToProcess);
					}
				};

				dialog.add(new VisLabel("Save all changes and crop sprites?")).left();
				dialog.add(new VisLabel("Caution: This process can take up to few minutes")).left();
				dialog.show(stage);
				return true;
			}

			case MessageType.EDITOR_SHOW_IMPORT_FILE_DIALOG: {
				ShowImportFileDialogMessage message = (ShowImportFileDialogMessage) msg.extraInfo;
				VisValidatableTextField filenameTextField = new VisValidatableTextField();
				filenameTextField.addValidator(new InputValidator() {
					@Override
					public boolean validateInput(String input) {
						if (StringUtils.isBlank(input)) {
							return false;
						}
						return FileUtils.findFilesByFilename(editorStateProvider.getState().getModDirPath(), input).isEmpty();
					}
				});
				if (message.suggestedFilename() != null) {
					filenameTextField.setText(message.suggestedFilename());
				}
				OkCancelDialog dialog = new OkCancelDialog("Import file into mods") {
					@Override
					public void onOk() {
						Path source = message.originalFileHandle().file().toPath();
						Path target = Path.of(message.destinationDirectory().path(), filenameTextField.getText());
						FileUtils.copy(source, target);
						message.callback().accept(new FileHandle(target.toFile()));
					}
				};

				dialog.add(new VisLabel("Original file")).left();
				dialog.add(new VisLabel(FileUtils.getDirectory(message.originalFileHandle().file().toPath()).toString())).left();
				dialog.add(new VisLabel(message.originalFileHandle().name())).left();
				dialog.row();
				dialog.add(new VisLabel("New Filename")).left();
				dialog.add(new VisLabel(message.destinationDirectory().path())).left();
				dialog.add(filenameTextField).minWidth(350).left();
				dialog.show(stage);
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
