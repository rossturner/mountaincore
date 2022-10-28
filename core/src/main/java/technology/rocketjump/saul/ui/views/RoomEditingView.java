package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.rooms.components.RoomComponent;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;
import technology.rocketjump.saul.ui.widgets.TextInputDialog;

@Singleton
public class RoomEditingView implements GuiView, GameContextAware, DisplaysText, Telegraph {

	private static final int FURNITURE_PER_ROW = 9;

	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer interactionStateContainer;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final RoomEditorFurnitureMap furnitureMap;
	private final EntityRenderer entityRenderer;
	private final RoomStore roomStore;
	private final Table headerContainer;
	private final Button changeRoomNameButton;
	private final Table sizingButtons;
	private GameContext gameContext;

	private Button backButton;
	private Table mainTable;
	private boolean displayed;

	private FurnitureType selectedFurnitureType;
	private GameMaterialType selectedFurnitureMaterialType;
	private GameMaterial selectedFurniturePrimaryMaterial;

	@Inject
	public RoomEditingView(MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository,
						   I18nTranslator i18nTranslator, GameInteractionStateContainer interactionStateContainer,
						   FurnitureTypeDictionary furnitureTypeDictionary, RoomEditorFurnitureMap furnitureMap, EntityRenderer entityRenderer, RoomStore roomStore) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		skin = skinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureMap = furnitureMap;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.entityRenderer = entityRenderer;
		this.roomStore = roomStore;

		backButton = new Button(skin.getDrawable("btn_back"));
		mainTable = new Table();
		mainTable.setDebug(GlobalSettings.UI_DEBUG);
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.getDrawable("asset_dwarf_select_bg"));
		mainTable.pad(20);


		headerContainer = new Table();
//		headerContainer.setDebug(GlobalSettings.UI_DEBUG);
		headerContainer.setBackground(skin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));

		changeRoomNameButton = new Button(skin.getDrawable("icon_edit"));
		changeRoomNameButton.addListener(new ClickListener() {

			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (getSelectedRoom() != null) {
					// Grabbing translations here so they're always for the correct language
					I18nText renameRoomDialogTitle = i18nTranslator.getTranslatedString("GUI.DIALOG.RENAME_ROOM_TITLE");
					I18nText descriptionText = i18nTranslator.getTranslatedString("RENAME_DESC");
					I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

					final boolean performPause = !gameContext.getGameClock().isPaused();
					if (performPause) {
						messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
					}

					String originalRoomName = getSelectedRoom().getRoomName();

					TextInputDialog textInputDialog = new TextInputDialog(renameRoomDialogTitle, descriptionText, originalRoomName, buttonText, skin, (newRoomName) -> {
						if (performPause) {
							messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
						}
						if (!originalRoomName.equals(newRoomName)) {
							try {
								roomStore.rename(getSelectedRoom(), newRoomName);
								rebuildUI();
							} catch (RoomStore.RoomNameCollisionException e) {
//								ModalDialog errorDialog = gameDialogDictionary.getErrorDialog(ErrorType.ROOM_NAME_ALREADY_EXISTS);
//								messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, errorDialog);
							}
						}
					}, messageDispatcher);
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
				}
			}
		});
		tooltipFactory.simpleTooltip(changeRoomNameButton, "GUI.DIALOG.RENAME_ROOM_TITLE", TooltipLocationHint.ABOVE);
		changeRoomNameButton.addListener(new ChangeCursorOnHover(changeRoomNameButton, GameCursor.SELECT, messageDispatcher));

		sizingButtons = new Table();

		messageDispatcher.addListener(this, MessageType.GUI_ROOM_TYPE_SELECTED);
		messageDispatcher.addListener(this, MessageType.CHOOSE_SELECTABLE);
		messageDispatcher.addListener(this, MessageType.INTERACTION_MODE_CHANGED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.INTERACTION_MODE_CHANGED -> {
				if (displayed) {
					rebuildUI();
				}
				return true;
			}
			case MessageType.GUI_ROOM_TYPE_SELECTED -> rebuildUI();
			case MessageType.CHOOSE_SELECTABLE -> {
				Selectable selectable = (Selectable) msg.extraInfo;
				if (selectable.getRoom() != null) {
					rebuildUI();
				}
			}
		}
		return false;
	}

	@Override
	public void rebuildUI() {
		backButton.clearListeners();
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
			}
		});
		backButton.addListener(new ChangeCursorOnHover(backButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(backButton, "GUI.BACK_LABEL", TooltipLocationHint.ABOVE);

		mainTable.clearChildren();

		headerContainer.clearChildren();

		Room selectedRoom = getSelectedRoom();
		RoomType selectedRoomType = selectedRoom != null ? selectedRoom.getRoomType() : interactionStateContainer.getSelectedRoomType();
		if (selectedRoom == null && selectedRoomType == null) {
			return;
		}

		String headerText = selectedRoom != null ? selectedRoom.getRoomName() : i18nTranslator.getTranslatedString(selectedRoomType.getI18nKey()).toString();
		Label headerLabel = new Label(headerText, skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(new Container<>()).left().expandX().width(changeRoomNameButton.getWidth());
		headerContainer.add(headerLabel).center();
		if (selectedRoom != null) {
			headerContainer.add(changeRoomNameButton).right().expandX().width(changeRoomNameButton.getWidth());
		} else {
			headerContainer.add(new Container<>()).right().expandX().width(changeRoomNameButton.getWidth());
		}

		buildSizingButtonsTable();
		Container<Table> sizingButtonsContainer = new Container<>();
		sizingButtonsContainer.setActor(sizingButtons);
		sizingButtonsContainer.right();

		Table topRow = new Table();
		topRow.setDebug(true);
		topRow.add(new Container<>()).left().expandX().width(sizingButtonsContainer.getWidth());
		topRow.add(headerContainer).center().expandY();
		topRow.add(sizingButtonsContainer).right().expandX().width(sizingButtonsContainer.getWidth());

		mainTable.add(topRow).top().expandX().fillX().padBottom(20).row();

		if (selectedRoom != null) {
			for (RoomComponent roomComponent : selectedRoom.getAllComponents()) {
				if (roomComponent instanceof SelectableDescription) {
					for (I18nText description : ((SelectableDescription) roomComponent).getDescription(i18nTranslator, gameContext, messageDispatcher)) {
						Label textLabel = new Label(description.toString(), skin.get("default-red", Label.LabelStyle.class));
						mainTable.add(textLabel).center().row();
					}
				}
				if (roomComponent instanceof Prioritisable prioritisableComponent) {

					Table priorityTable = new Table();
					priorityTable.setDebug(GlobalSettings.UI_DEBUG);
					priorityTable.pad(4);

					// TODO grab buttons from skin with up and checked/hover states for each priority, setting current priority as checked
					for (JobPriority priority : JobPriority.values()) {

					}
				}
			}
		}

		// TODO seed selection if farm plot

		if (!selectedRoomType.getFurnitureNames().isEmpty()) {
			int furnitureCursor = 0;
			Table furnitureTable = new Table();
			furnitureTable.defaults().padRight(5);
			furnitureTable.setDebug(GlobalSettings.UI_DEBUG);

			for (String furnitureName : selectedRoomType.getFurnitureNames()) {
				FurnitureType furnitureType = furnitureTypeDictionary.getByName(furnitureName);
				if (furnitureType == null) {
					Logger.error("Could not find furniture type with name " + furnitureName + " for room " + selectedRoomType.getRoomName());
				} else if (!furnitureType.isHiddenFromPlacementMenu()) {
					furnitureTable.add(buildFurnitureButton(furnitureType));
					furnitureCursor++;
					if (furnitureCursor % FURNITURE_PER_ROW == 0) {
						furnitureTable.row();
					}
				}
			}

			// TODO get place in any room furniture and also add

			while (furnitureCursor % FURNITURE_PER_ROW != 0) {
				Container<Image> spacerContainer = new Container<>();
				Image spacerImage = new Image(skin.getDrawable("asset_catalogue_bg"));
				spacerContainer.setActor(spacerImage);
				spacerContainer.pad(18);
				furnitureTable.add(spacerContainer);
				furnitureCursor++;
			}

			mainTable.add(furnitureTable).row();
		}

	}

	private Actor buildFurnitureButton(FurnitureType furnitureType) {
		Container<Button> buttonContainer = new Container<>();
		if (furnitureType.equals(selectedFurnitureType)) {
			buttonContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		buttonContainer.pad(18);

		Drawable background = skin.getDrawable("asset_bg");
		Button furnitureButton = new Button(new EntityDrawable(
				furnitureMap.getByFurnitureType(furnitureType), entityRenderer, true, messageDispatcher
		).withBackground(background));
		buttonContainer.size(background.getMinWidth(), background.getMinHeight());
		var This = this;
		furnitureButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				This.selectedFurnitureType = furnitureType;
				GameInteractionMode.PLACE_FURNITURE.setFurnitureType(selectedFurnitureType);
				messageDispatcher.dispatchMessage(MessageType.GUI_FURNITURE_TYPE_SELECTED, selectedFurnitureType);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FURNITURE);
				rebuildUI();
			}
		});
		furnitureButton.addListener(new ChangeCursorOnHover(furnitureButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(furnitureButton, furnitureType.getI18nKey(), TooltipLocationHint.ABOVE);

		buttonContainer.setActor(furnitureButton);
		return buttonContainer;
	}

	private void buildSizingButtonsTable() {
		sizingButtons.clearChildren();
		Container<Button> addTilesContainer = new Container<>();
		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_ROOM)) {
			addTilesContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		addTilesContainer.pad(18);
		Button addTilesButton = new Button(skin.getDrawable("btn_add_tile"));
		addTilesButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_ROOM);
			}
		});
		addTilesButton.addListener(new ChangeCursorOnHover(addTilesContainer, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(addTilesButton, "GUI.ADD_TILES", TooltipLocationHint.ABOVE);
		addTilesContainer.setActor(addTilesButton);
		sizingButtons.add(addTilesContainer);

		Container<Button> removeTilesContainer = new Container<>();
		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_ROOMS)) {
			removeTilesContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		removeTilesContainer.pad(18);
		Button removeTilesButton = new Button(skin.getDrawable("btn_remove_tile"));
		removeTilesButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.REMOVE_ROOMS);
			}
		});
		removeTilesButton.addListener(new ChangeCursorOnHover(removeTilesContainer, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(removeTilesButton, "GUI.REMOVE_TILES", TooltipLocationHint.ABOVE);
		removeTilesContainer.setActor(removeTilesButton);
		sizingButtons.add(removeTilesContainer).padRight(20);
	}

	private Room getSelectedRoom() {
		if (interactionStateContainer.getSelectable() != null) {
			return interactionStateContainer.getSelectable().getRoom();
		} else {
			return null;
		}
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();
		if (!getParentViewName().equals(GuiViewName.DEFAULT_MENU)) {
			containerTable.add(backButton).left().bottom().padLeft(30).padRight(50);
		}
		containerTable.add(mainTable);
	}

	@Override
	public void onShow() {
		this.displayed = true;
		rebuildUI();
	}

	@Override
	public void onHide() {
		this.displayed = false;
		this.selectedFurnitureType = null;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.selectedFurnitureType = null;
	}

	@Override
	public void update() {

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ROOM_EDITING;
	}

	@Override
	public GuiViewName getParentViewName() {
		if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().getRoom() != null) {
			return GuiViewName.DEFAULT_MENU;
		} else {
			return GuiViewName.ROOM_SELECTION;
		}
	}
}
