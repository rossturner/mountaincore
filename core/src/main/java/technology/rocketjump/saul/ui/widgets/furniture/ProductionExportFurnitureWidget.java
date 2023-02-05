package technology.rocketjump.saul.ui.widgets.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.crafting.CraftingRecipeDictionary;
import technology.rocketjump.saul.crafting.model.CraftingRecipe;
import technology.rocketjump.saul.entities.behaviour.furniture.ProductionExportFurnitureBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.TradingImportFurnitureBehaviour;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.entities.tags.CraftingStationBehaviourTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.views.RoomEditorItemMap;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;
import technology.rocketjump.saul.ui.widgets.SelectItemDialog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ProductionExportFurnitureWidget extends Table implements DisplaysText, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final RoomEditorItemMap roomEditorItemMap;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final EntityRenderer entityRenderer;
	private final I18nTranslator i18nTranslator;
	private final SoundAssetDictionary soundAssetDictionary;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final CraftingTypeDictionary craftingTypeDictionary;
	private final CraftingRecipeDictionary craftingRecipeDictionary;
	private final GuiSkinRepository guiSkinRepository;
	private final ItemTypeDictionary itemTypeDictionary;

	private final Container<Button> buttonContainer = new Container<>();
	private final Drawable noneSelectedDrawable;
	private final Drawable backgroundDrawable;
	private final Button leftButton, rightButton;
	private final MainGameSkin skin;


	private Entity furnitureEntity;
	private ProductionExportFurnitureBehaviour productionExportBehaviour;
	private GameContext gameContext;

	private List<GameMaterial> availableMaterials;


	@Inject
	public ProductionExportFurnitureWidget(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
										   TooltipFactory tooltipFactory, RoomEditorItemMap roomEditorItemMap,
										   GameMaterialDictionary gameMaterialDictionary, EntityRenderer entityRenderer,
										   I18nTranslator i18nTranslator,
										   SoundAssetDictionary soundAssetDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
										   CraftingTypeDictionary craftingTypeDictionary, CraftingRecipeDictionary craftingRecipeDictionary,
										   ItemTypeDictionary itemTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.guiSkinRepository = guiSkinRepository;
		this.tooltipFactory = tooltipFactory;
		this.roomEditorItemMap = roomEditorItemMap;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.entityRenderer = entityRenderer;
		this.i18nTranslator = i18nTranslator;
		this.soundAssetDictionary = soundAssetDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.craftingRecipeDictionary = craftingRecipeDictionary;

		skin = guiSkinRepository.getMainGameSkin();
		this.itemTypeDictionary = itemTypeDictionary;

		backgroundDrawable = skin.getDrawable("asset_bg");
		buttonContainer.setBackground(backgroundDrawable);

		leftButton = new Button(skin.get("btn_arrow_small_left", Button.ButtonStyle.class));
		rightButton = new Button(skin.get("btn_arrow_small_right", Button.ButtonStyle.class));

		noneSelectedDrawable = skin.getDrawable("icon_not_equipped_no_bg");
	}

	public void setFurnitureEntity(Entity entity) {
		if (entity.getBehaviourComponent() instanceof ProductionExportFurnitureBehaviour productionExportFurnitureBehaviour) {
			this.furnitureEntity = entity;
			this.productionExportBehaviour = productionExportFurnitureBehaviour;
			rebuildUI();
		} else {
			Logger.error("Entity {} passed to {} is not a {}", entity, getClass().getSimpleName(), ProductionExportFurnitureBehaviour.class.getSimpleName());
			furnitureEntity = null;
			productionExportBehaviour = null;
			rebuildUI();
		}
	}

	@Override
	public void rebuildUI() {
		clearChildren();
		if (furnitureEntity == null || productionExportBehaviour == null) {
			return;
		}

		ItemType selectedItemType = productionExportBehaviour.getSelectedItemType();

		// Material selection
		determineAvailableMaterials(selectedItemType);

		Button button;
		if (selectedItemType == null) {
			button = new Button(noneSelectedDrawable);
			tooltipFactory.simpleTooltip(button, "ITEM.NONE_SELECTED", TooltipLocationHint.ABOVE);
		} else {
			Entity displayedEntity = roomEditorItemMap.getByItemType(selectedItemType);
			GameMaterial selectedMaterial = productionExportBehaviour.getSelectedMaterial();
			if (selectedMaterial == null) {
				selectedMaterial = gameMaterialDictionary.getExampleMaterial(selectedItemType.getPrimaryMaterialType());
			}
			ItemEntityAttributes attributes = (ItemEntityAttributes) displayedEntity.getPhysicalEntityComponent().getAttributes();
			attributes.setMaterial(selectedMaterial);
			attributes.setQuantity(selectedItemType.getMaxStackSize());
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, displayedEntity);

			EntityDrawable entityDrawable = new EntityDrawable(displayedEntity, entityRenderer, true, messageDispatcher);
			entityDrawable.setMinSize(backgroundDrawable.getMinWidth(), backgroundDrawable.getMinHeight());
			button = new Button(entityDrawable);
			tooltipFactory.simpleTooltip(button, selectedItemType.getI18nKey(), TooltipLocationHint.ABOVE);
		}

		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ProductionExportFurnitureWidget.this.onClickItemType();
			}
		});
		button.addListener(new ChangeCursorOnHover(button, GameCursor.SELECT, messageDispatcher));
		buttonContainer.setActor(button);

		this.add(buttonContainer).center().row();

		leftButton.clearListeners();
		rightButton.clearListeners();
		if (availableMaterials.size() > 1) {
			leftButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					previousMaterialSelection();
				}
			});
			leftButton.addListener(new ChangeCursorOnHover(leftButton, GameCursor.SELECT, messageDispatcher));
			leftButton.setDisabled(false);
			rightButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					nextMaterialSelection();
				}
			});
			rightButton.addListener(new ChangeCursorOnHover(rightButton, GameCursor.SELECT, messageDispatcher));
			rightButton.setDisabled(false);
		} else {
			leftButton.setDisabled(true);
			rightButton.setDisabled(true);
		}

		if (availableMaterials.size() == 1) {
			productionExportBehaviour.setSelectedMaterial(availableMaterials.get(0));
		}
		GameMaterial selectedMaterial = productionExportBehaviour.getSelectedMaterial();
		String materialI18nKey = selectedMaterial == null ? "MATERIAL_TYPE.ANY" : selectedMaterial.getI18nKey();
		Label materialLabel = new Label(i18nTranslator.getTranslatedString(materialI18nKey).toString(), skin.get("default-red", Label.LabelStyle.class));
		materialLabel.setAlignment(Align.center);
		Table materialRow = new Table();
		materialRow.add(leftButton).left().padRight(5);
		materialRow.add(materialLabel).center().growX();
		materialRow.add(rightButton).right().padLeft(5);
		this.add(materialRow).center().growX().padTop(4).row();
	}

	private void determineAvailableMaterials(ItemType selectedItemType) {
		availableMaterials = new ArrayList<>();
		if (selectedItemType == null) {
			availableMaterials.add(0, null);
		} else {
			List<GameMaterial> specifiedMaterials = selectedItemType.getSpecificallyAllowedMaterials(craftingRecipeDictionary);

			if (specifiedMaterials.isEmpty() || specifiedMaterials.contains(null)) {
				availableMaterials.addAll(gameMaterialDictionary.getByType(selectedItemType.getPrimaryMaterialType()).stream()
						.filter(m -> !m.isHiddenFromUI()).toList());
				availableMaterials.sort(Comparator.comparing(m -> i18nTranslator.getTranslatedString(m.getI18nKey()).toString()));
				if (availableMaterials.size() > 1) {
					availableMaterials.add(0, null);
				}
			} else {
				availableMaterials.addAll(specifiedMaterials);
				availableMaterials.sort(Comparator.comparing(m -> i18nTranslator.getTranslatedString(m.getI18nKey()).toString()));
				if (availableMaterials.size() > 1) {
					availableMaterials.add(0, null);
				}
				if (!availableMaterials.contains(productionExportBehaviour.getSelectedMaterial())) {
					productionExportBehaviour.setSelectedMaterial(availableMaterials.get(0));
				}
			}
		}
	}

	private void previousMaterialSelection() {
		GameMaterial previousMaterial = availableMaterials.get(availableMaterials.size() - 1);

		for (GameMaterial availableMaterial : availableMaterials) {
			if (availableMaterial == productionExportBehaviour.getSelectedMaterial()) {
				break;
			} else {
				previousMaterial = availableMaterial;
			}
		}

		productionExportBehaviour.setSelectedMaterial(previousMaterial);
		rebuildUI();
	}

	private void nextMaterialSelection() {
		int nextIndex = availableMaterials.indexOf(productionExportBehaviour.getSelectedMaterial()) + 1;
		if (nextIndex == availableMaterials.size()) {
			nextIndex = 0;
		}
		productionExportBehaviour.setSelectedMaterial(availableMaterials.get(nextIndex));
		rebuildUI();
	}

	private void onClickItemType() {
		MapTile tile = gameContext.getAreaMap().getTile(furnitureEntity.getLocationComponent().getWorldOrParentPosition());
		if (tile == null || tile.getRoomTile() == null) {
			Logger.error("No room tile found under furniture entity {}", furnitureEntity);
			return;
		}
		RoomType currentRoomType = tile.getRoomTile().getRoom().getRoomType();

		List<ItemType> craftingOutputItems = getSelectableItemTypes(currentRoomType);

		List<SelectItemDialog.Option> options = new ArrayList<>();
		craftingOutputItems.forEach(itemType -> {
			options.add(new SelectItemTypeOption(itemType, () -> {
				if (itemType != productionExportBehaviour.getSelectedItemType()) {
					productionExportBehaviour.setSelectedItemType(itemType);
					productionExportBehaviour.setSelectedMaterial(null);
					rebuildUI();
				}
			}));
		});
		options.add(new SelectItemDialog.Option(i18nTranslator.getTranslatedString("ITEM.NONE_SELECTED")) {
			@Override
			public void addSelectionComponents(Table innerTable) {
				Image image = new Image(noneSelectedDrawable);
				innerTable.add(image).size(183, 183).pad(10).row();
			}

			@Override
			public void onSelect() {
				productionExportBehaviour.setSelectedItemType(null);
				productionExportBehaviour.setSelectedMaterial(null);
				rebuildUI();
				messageDispatcher.dispatchMessage(MessageType.GUI_REMOVE_ALL_TOOLTIPS);
			}
		});

		String key = furnitureEntity.getBehaviourComponent() instanceof TradingImportFurnitureBehaviour ? "GUI.PRODUCTION_IMPORT.CHOOSE_ITEM_TYPE" : "GUI.PRODUCTION_EXPORT.CHOOSE_ITEM_TYPE";
		SelectItemDialog selectItemDialog = new SelectItemDialog(i18nTranslator.getTranslatedString(key),
				guiSkinRepository.getMenuSkin(), messageDispatcher, soundAssetDictionary, tooltipFactory, options, SelectItemDialog.ITEMS_PER_ROW);
		selectItemDialog.getContentTable().padLeft(60);
		selectItemDialog.setShowWithAnimation(false);
		messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, selectItemDialog);
	}

	private List<ItemType> getSelectableItemTypes(RoomType currentRoomType) {
		if (furnitureEntity.getBehaviourComponent() instanceof TradingImportFurnitureBehaviour) {
			return Stream.concat(
						craftingRecipeDictionary.getAll().stream()
								.map(CraftingRecipe::getOutput)
								.map(QuantifiedItemTypeWithMaterial::getItemType)
								.filter(Objects::nonNull),
						itemTypeDictionary.getTradeableItems().stream()
					)
					.collect(Collectors.toSet()).stream()
					.sorted(Comparator.comparing(a -> i18nTranslator.getTranslatedString(a.getI18nKey()).toString()))
					.toList();
		} else {
			return currentRoomType.getFurnitureNames().stream()
					.map(furnitureTypeDictionary::getByName)
					.flatMap(f -> f.getProcessedTags().stream())
					.filter(t -> t instanceof CraftingStationBehaviourTag)
					.map(t -> (CraftingStationBehaviourTag) t)
					.map(c -> c.getCraftingType(craftingTypeDictionary))
					.flatMap(c -> craftingRecipeDictionary.getByCraftingType(c).stream())
					.map(r -> r.getOutput().getItemType())
					.filter(Objects::nonNull)
					.collect(Collectors.toSet()).stream()
					.sorted(Comparator.comparing(a -> i18nTranslator.getTranslatedString(a.getI18nKey()).toString()))
					.toList();
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.furnitureEntity = null;
		this.productionExportBehaviour = null;
	}


	class SelectItemTypeOption extends SelectItemDialog.Option {

		private final Drawable drawable;
		private final Runnable onSelection;

		public SelectItemTypeOption(ItemType itemType, Runnable onSelection) {
			super(i18nTranslator.getTranslatedString(itemType.getI18nKey()));
			Entity itemEntity = roomEditorItemMap.getByItemType(itemType);
			((ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes()).setQuantity(itemType.getMaxStackSize());
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemEntity);
			this.drawable = new EntityDrawable(itemEntity, entityRenderer, true, messageDispatcher);
			this.onSelection = onSelection;
		}

		@Override
		public void addSelectionComponents(Table innerTable) {
			Image image = new Image(drawable);
			innerTable.add(image).size(183, 183).pad(10).row();
			innerTable.add(new Container<>()).height(30).row();
		}

		@Override
		public void onSelect() {
			onSelection.run();
			messageDispatcher.dispatchMessage(MessageType.GUI_REMOVE_ALL_TOOLTIPS);
		}
	}

}
