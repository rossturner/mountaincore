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
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.async.ErrorType;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.*;
import technology.rocketjump.saul.rooms.components.FarmPlotComponent;
import technology.rocketjump.saul.rooms.components.RoomComponent;
import technology.rocketjump.saul.rooms.components.StockpileRoomComponent;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.*;
import technology.rocketjump.saul.ui.widgets.furniture.FurnitureRequirementsWidget;
import technology.rocketjump.saul.ui.widgets.rooms.FarmPlotDescriptionWidget;
import technology.rocketjump.saul.ui.widgets.rooms.FarmPlotWidget;
import technology.rocketjump.saul.ui.widgets.rooms.PriorityWidget;

@Singleton
public class RoomEditingView implements GuiView, GameContextAware, DisplaysText, Telegraph {

	public static final int FURNITURE_PER_ROW = 9;

	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer interactionStateContainer;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final RoomEditorFurnitureMap furnitureMap;
	private final EntityRenderer entityRenderer;
	private final RoomEditorItemMap itemMap;
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private final Table headerContainer;
	private final Button changeRoomNameButton;
	private final Table sizingButtons;
	private final FurnitureRequirementsWidget furnitureRequirementsWidget;
	private final RoomFactory roomFactory;
	private final GameMaterialDictionary materialDictionary;
	private final GameDialogDictionary gameDialogDictionary;
	private final ButtonFactory buttonFactory;
	private GameContext gameContext;

	private Button backButton;
	private final Table mainTable;
	private boolean displayed;

	private boolean stockpileSettingsExpanded;
	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final RaceDictionary raceDictionary;
	private final SoundAssetDictionary soundAssetDictionary;

	@Inject
	public RoomEditingView(MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository,
						   I18nTranslator i18nTranslator, GameInteractionStateContainer interactionStateContainer,
						   FurnitureTypeDictionary furnitureTypeDictionary, RoomEditorFurnitureMap furnitureMap,
						   EntityRenderer entityRenderer, RoomStore roomStore, RoomEditorItemMap itemMap,
						   PlantSpeciesDictionary plantSpeciesDictionary, FurnitureRequirementsWidget furnitureRequirementsWidget,
						   RoomFactory roomFactory, GameMaterialDictionary materialDictionary, GameDialogDictionary gameDialogDictionary,
						   ButtonFactory buttonFactory, StockpileComponentUpdater stockpileComponentUpdater, StockpileGroupDictionary stockpileGroupDictionary,
						   ItemTypeDictionary itemTypeDictionary, RaceDictionary raceDictionary, SoundAssetDictionary soundAssetDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		skin = skinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureMap = furnitureMap;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.entityRenderer = entityRenderer;
		this.itemMap = itemMap;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.furnitureRequirementsWidget = furnitureRequirementsWidget;
		this.roomFactory = roomFactory;
		this.materialDictionary = materialDictionary;
		this.gameDialogDictionary = gameDialogDictionary;
		this.buttonFactory = buttonFactory;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.raceDictionary = raceDictionary;
		this.soundAssetDictionary = soundAssetDictionary;

		mainTable = new Table();
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.getDrawable("asset_dwarf_select_bg"));
		mainTable.pad(20);
		mainTable.defaults().padBottom(20);
		mainTable.top();


		headerContainer = new Table();
		headerContainer.setBackground(skin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));

		changeRoomNameButton = buttonFactory.buildDrawableButton("icon_edit", "GUI.DIALOG.RENAME_ROOM_TITLE", () -> {
			if (getSelectedRoom() != null) {
				// Grabbing translations here so they're always for the correct language
				I18nText renameRoomDialogTitle = i18nTranslator.getTranslatedString("GUI.DIALOG.RENAME_ROOM_TITLE");
				I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

				final boolean performPause = !gameContext.getGameClock().isPaused();
				if (performPause) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
				}

				String originalRoomName = getSelectedRoom().getRoomName();

				TextInputDialog textInputDialog = new TextInputDialog(renameRoomDialogTitle, originalRoomName, buttonText, skinRepository.getMenuSkin(), (newRoomName) -> {
					if (performPause) {
						messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
					}
					if (!originalRoomName.equals(newRoomName)) {
						try {
							roomStore.rename(getSelectedRoom(), newRoomName);
							rebuildUI();
						} catch (RoomStore.RoomNameCollisionException e) {
							ModalDialog errorDialog = RoomEditingView.this.gameDialogDictionary.getErrorDialog(ErrorType.ROOM_NAME_ALREADY_EXISTS);
							messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, errorDialog);
						}
					}
				}, messageDispatcher, RoomEditingView.this.soundAssetDictionary);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
			}
		});

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
			case MessageType.GUI_ROOM_TYPE_SELECTED -> {
				stockpileSettingsExpanded = false;
				rebuildUI();
			}
			case MessageType.CHOOSE_SELECTABLE -> {
				Selectable selectable = (Selectable) msg.extraInfo;
				if (selectable.getRoom() != null) {
					stockpileSettingsExpanded = false;
					rebuildUI();
				}
			}
		}
		return false;
	}

	@Override
	public void rebuildUI() {
		backButton = buttonFactory.buildDrawableButton("btn_back", "GUI.BACK_LABEL", () -> {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
				interactionStateContainer.setSelectable(null);
		});

		mainTable.clearChildren();

		headerContainer.clearChildren();

		Room selectedRoom = getSelectedRoom();
		RoomType selectedRoomType = getSelectedRoomType();
		if (selectedRoom == null && selectedRoomType == null) {
			return;
		}

		String headerText;
		if (selectedRoom != null) {
			headerText = selectedRoom.getRoomName();
		} else if (selectedRoomType.isStockpile()) {
			headerText = roomFactory.getStockpileRoomName(selectedRoomType, interactionStateContainer.getSelectedStockpileGroup()).toString();
		} else {
			headerText = i18nTranslator.getTranslatedString(selectedRoomType.getI18nKey()).toString();
		}
		Label headerLabel = new Label(headerText, skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(new Container<>()).left().expandX().width(changeRoomNameButton.getWidth());
		headerContainer.add(headerLabel).center();
		if (selectedRoom != null) {
			headerContainer.add(changeRoomNameButton).right().expandX().width(changeRoomNameButton.getWidth());
		} else {
			headerContainer.add(new Container<>()).right().expandX().width(changeRoomNameButton.getWidth());
		}

		buildSizingButtonsTable(selectedRoom);
		Container<Table> sizingButtonsContainer = new Container<>();
		sizingButtonsContainer.setActor(sizingButtons);
		sizingButtonsContainer.right();

		Table topRow = new Table();
		topRow.add(new Container<>()).left().expandX().width(400);
		topRow.add(headerContainer).center().width(900).expandY();
		topRow.add(sizingButtonsContainer).right().expandX().width(400);

		mainTable.add(topRow).top().expandX().fillX().row();

		if (selectedRoom != null) {
			FarmPlotComponent farmPlotComponent = selectedRoom.getComponent(FarmPlotComponent.class);
			FarmPlotDescriptionWidget farmPlotDescriptionWidget = new FarmPlotDescriptionWidget(farmPlotComponent, i18nTranslator, skin);

			if (farmPlotComponent != null) {
				mainTable.add(farmPlotDescriptionWidget).center().row();
			}

			for (RoomComponent roomComponent : selectedRoom.getAllComponents()) {
				if (roomComponent instanceof SelectableDescription) {
					for (I18nText description : ((SelectableDescription) roomComponent).getDescription(i18nTranslator, gameContext, messageDispatcher)) {
						Label textLabel = new Label(description.toString(), skin.get("default-red", Label.LabelStyle.class));
						mainTable.add(textLabel).center().row();
					}
				}
				if (roomComponent instanceof Prioritisable prioritisableComponent) {
					mainTable.add(new PriorityWidget(prioritisableComponent, skin, tooltipFactory, messageDispatcher, soundAssetDictionary)).center().row();
				}
			}

			if (farmPlotComponent != null) {
				FarmPlotWidget farmPlotWidget = new FarmPlotWidget(selectedRoom, farmPlotComponent, skin, tooltipFactory, messageDispatcher,
						plantSpeciesDictionary, itemMap, entityRenderer, i18nTranslator, soundAssetDictionary);
				farmPlotWidget.setOnSeedChange(farmPlotDescriptionWidget::cropChanged);
				mainTable.add(farmPlotWidget).center().row();
			}

			StockpileRoomComponent stockpileComponent = selectedRoom.getComponent(StockpileRoomComponent.class);
			if (stockpileComponent != null && stockpileSettingsExpanded) {
				StockpileManagementTree stockpileManagementTree = new StockpileManagementTree(skin, messageDispatcher,
						stockpileComponentUpdater, stockpileGroupDictionary,
						i18nTranslator, itemTypeDictionary, materialDictionary, raceDictionary,
						gameContext.getSettlementState().getSettlerRace(), selectedRoom.getRoomId(),
						HaulingAllocation.AllocationPositionType.ROOM, stockpileComponent.getStockpileSettings(), soundAssetDictionary);
				stockpileManagementTree.setSize(1700, 600);
				mainTable.add(stockpileManagementTree).left().row();
			}
		}

		if (!selectedRoomType.getFurnitureNames().isEmpty()) {
			int furnitureCursor = 0;
			Table furnitureTable = new Table();
			furnitureTable.defaults().padRight(5);

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

			mainTable.add(furnitureTable).center().row();
		}

		if (interactionStateContainer.getFurnitureTypeToPlace() != null) {
			mainTable.add(furnitureRequirementsWidget).center().expandX().row();

			Entity furnitureEntity = furnitureMap.getByFurnitureType(interactionStateContainer.getFurnitureTypeToPlace());
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
			furnitureRequirementsWidget.onMaterialSelection(material -> {
				if (material != null) {
					attributes.setMaterial(material);
					messageDispatcher.dispatchMessage(MessageType.FURNITURE_MATERIAL_SELECTED);
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
				}
			});
			furnitureRequirementsWidget.onMaterialTypeSelection(materialType -> {
				attributes.setPrimaryMaterialType(materialType);
				if (!attributes.getMaterials().containsKey(materialType)) {
					attributes.setMaterial(materialDictionary.getExampleMaterial(materialType));
				}
				messageDispatcher.dispatchMessage(MessageType.FURNITURE_MATERIAL_SELECTED);
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
			});
		}
	}

	private RoomType getSelectedRoomType() {
		Room selectedRoom = getSelectedRoom();
		return selectedRoom != null ? selectedRoom.getRoomType() : interactionStateContainer.getSelectedRoomType();
	}

	private Actor buildFurnitureButton(FurnitureType furnitureType) {
		Container<Button> buttonContainer = new Container<>();
		if (furnitureType.equals(interactionStateContainer.getFurnitureTypeToPlace())) {
			buttonContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		buttonContainer.pad(18);

		Drawable background = skin.getDrawable("asset_bg");
		Button furnitureButton = new Button(new EntityDrawable(
				furnitureMap.getByFurnitureType(furnitureType), entityRenderer, true, messageDispatcher
		).withBackground(background));
		buttonContainer.size(background.getMinWidth(), background.getMinHeight());
		buttonFactory.attachClickCursor(furnitureButton, GameCursor.SELECT);
		furnitureButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				interactionStateContainer.setFurnitureTypeToPlace(furnitureType);
				furnitureRequirementsWidget.changeSelectedFurniture(furnitureType);
				rebuildUI();
				GameInteractionMode.PLACE_FURNITURE.setFurnitureType(interactionStateContainer.getFurnitureTypeToPlace());
				messageDispatcher.dispatchMessage(MessageType.GUI_FURNITURE_TYPE_SELECTED, interactionStateContainer.getFurnitureTypeToPlace());
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FURNITURE);
			}
		});

		tooltipFactory.withTooltipText(furnitureButton, furnitureType.getI18nKey(), TooltipFactory.TooltipBackground.LARGE_PATCH_DARK);

		buttonContainer.setActor(furnitureButton);
		return buttonContainer;
	}

	private void buildSizingButtonsTable(Room selectedRoom) {
		sizingButtons.clearChildren();

		if (selectedRoom != null && selectedRoom.getRoomType().isStockpile()) {
			Container<Button> stockpileSettingsContainer = new Container<>();
			if (stockpileSettingsExpanded) {
				stockpileSettingsContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
			}
			stockpileSettingsContainer.pad(18);
			Button stockpileSettingsButton = buttonFactory.buildDrawableButton("btn_settings", "GUI.DIALOG.STOCKPILE_MANAGEMENT.TITLE", this::toggleStockpileSettings);
			stockpileSettingsContainer.setActor(stockpileSettingsButton);
			sizingButtons.add(stockpileSettingsContainer);
		}

		Container<Button> addTilesContainer = new Container<>();
		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_ROOM)) {
			addTilesContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		addTilesContainer.pad(18);
		Button addTilesButton = buttonFactory.buildDrawableButton("btn_add_tile", "GUI.ADD_TILES", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_ROOM);
			interactionStateContainer.getInteractionMode().setRoomType(getSelectedRoomType());
		});
		addTilesContainer.setActor(addTilesButton);
		sizingButtons.add(addTilesContainer);

		Container<Button> removeTilesContainer = new Container<>();
		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_ROOMS)) {
			removeTilesContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		removeTilesContainer.pad(18);
		Button removeTilesButton = buttonFactory.buildDrawableButton("btn_remove_tile", "GUI.REMOVE_TILES", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.REMOVE_ROOMS);
		});
		removeTilesContainer.setActor(removeTilesButton);
		sizingButtons.add(removeTilesContainer).padRight(20);
	}

	private void toggleStockpileSettings() {
		this.stockpileSettingsExpanded = !this.stockpileSettingsExpanded;
		rebuildUI();
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
		containerTable.add(backButton).left().bottom().padLeft(30).padRight(50);
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
		this.interactionStateContainer.setFurnitureTypeToPlace(null);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.interactionStateContainer.setFurnitureTypeToPlace(null);
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
