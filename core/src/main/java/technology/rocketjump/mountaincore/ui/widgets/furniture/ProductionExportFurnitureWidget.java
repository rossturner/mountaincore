package technology.rocketjump.mountaincore.ui.widgets.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.crafting.CraftingRecipeDictionary;
import technology.rocketjump.mountaincore.crafting.model.CraftingRecipe;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.ProductionExportFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.TradingImportFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.tags.CraftingStationBehaviourTag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.rooms.RoomType;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.skins.MainGameSkin;
import technology.rocketjump.mountaincore.ui.views.RoomEditorItemMap;
import technology.rocketjump.mountaincore.ui.widgets.EntityDrawable;
import technology.rocketjump.mountaincore.ui.widgets.SelectItemDialog;
import technology.rocketjump.mountaincore.ui.widgets.crafting.CraftingHintWidgetFactory;

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
	private final CraftingHintWidgetFactory craftingHintWidgetFactory;
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
	private final MainGameSkin skin;

	private final SelectBox<GameMaterial> materialSelect;

	private Entity furnitureEntity;
	private ProductionExportFurnitureBehaviour productionExportBehaviour;
	private GameContext gameContext;

	private Array<GameMaterial> availableMaterials;


	@Inject
	public ProductionExportFurnitureWidget(MessageDispatcher messageDispatcher, CraftingHintWidgetFactory craftingHintWidgetFactory, GuiSkinRepository guiSkinRepository,
										   TooltipFactory tooltipFactory, RoomEditorItemMap roomEditorItemMap,
										   GameMaterialDictionary gameMaterialDictionary, EntityRenderer entityRenderer,
										   I18nTranslator i18nTranslator,
										   SoundAssetDictionary soundAssetDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
										   CraftingTypeDictionary craftingTypeDictionary, CraftingRecipeDictionary craftingRecipeDictionary,
										   ItemTypeDictionary itemTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.craftingHintWidgetFactory = craftingHintWidgetFactory;
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

		noneSelectedDrawable = skin.getDrawable("icon_not_equipped_no_bg");

		materialSelect = MaterialSelectBox.create(guiSkinRepository, i18nTranslator, messageDispatcher, soundAssetDictionary, this::changeMaterial);
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

		updateButton();

		this.add(buttonContainer).center().row();

		determineAvailableMaterials(selectedItemType);

		materialSelect.setItems(availableMaterials);

		if (availableMaterials.size == 1) {
			productionExportBehaviour.setSelectedMaterial(availableMaterials.get(0));
		}

		GameMaterial currentlySelected = productionExportBehaviour.getSelectedMaterial();
		materialSelect.setSelected(currentlySelected == null ? GameMaterial.NULL_MATERIAL : currentlySelected);

		this.add(materialSelect).center().growX().padTop(4).row();
	}

	private void updateButton() {
		ItemType selectedItemType = productionExportBehaviour.getSelectedItemType();
		Button button;
		if (selectedItemType == null) {
			button = new Button(noneSelectedDrawable);
			addNoneSelectedTooltip(button);
		} else {
			Entity displayedEntity = roomEditorItemMap.getByItemType(selectedItemType);
			GameMaterial selectedMaterial = productionExportBehaviour.getSelectedMaterial();
			GameMaterial tooltipMaterial = selectedMaterial;
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
			craftingHintWidgetFactory.addComplexTooltip(button, skin, selectedItemType, tooltipMaterial);
		}

		button.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		button.addListener(new ChangeCursorOnHover(button, GameCursor.SELECT, messageDispatcher));
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ProductionExportFurnitureWidget.this.onClickItemType();
			}
		});
		buttonContainer.setActor(button);
	}

	private void changeMaterial(GameMaterial selectedMaterial) {
		productionExportBehaviour.setSelectedMaterial(selectedMaterial);

		updateButton();
	}

	private void determineAvailableMaterials(ItemType selectedItemType) {
		availableMaterials = new Array<>();
		if (selectedItemType == null) {
			availableMaterials.add(GameMaterial.NULL_MATERIAL);
		} else {
			List<GameMaterial> specifiedMaterials = selectedItemType.getSpecificallyAllowedMaterials(craftingRecipeDictionary);

			if (specifiedMaterials.isEmpty() || specifiedMaterials.contains(null)) {
				availableMaterials.addAll(gameMaterialDictionary.getByType(selectedItemType.getPrimaryMaterialType()).stream()
						.filter(m -> !m.isHiddenFromUI()).toArray(GameMaterial[]::new));
				availableMaterials.sort(Comparator.comparing(m -> i18nTranslator.getTranslatedString(m.getI18nKey()).toString()));
				if (availableMaterials.size > 1) {
					availableMaterials.insert(0, GameMaterial.NULL_MATERIAL);
				}
			} else {
				availableMaterials.addAll(specifiedMaterials.toArray(GameMaterial[]::new));
				availableMaterials.sort(Comparator.comparing(m -> i18nTranslator.getTranslatedString(m.getI18nKey()).toString()));
				if (availableMaterials.size > 1) {
					availableMaterials.insert(0, GameMaterial.NULL_MATERIAL);
				}
				if (!availableMaterials.contains(productionExportBehaviour.getSelectedMaterial(), true)) {
					productionExportBehaviour.setSelectedMaterial(availableMaterials.get(0));
				}
			}
		}
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
		craftingOutputItems.forEach(itemType -> options.add(new SelectItemTypeOption(itemType, () -> {
			if (itemType != productionExportBehaviour.getSelectedItemType()) {
				productionExportBehaviour.setSelectedItemType(itemType);
				productionExportBehaviour.setSelectedMaterial(null);
				rebuildUI();
			}
		})));
		options.add(new SelectItemDialog.Option(I18nText.BLANK) {
			@Override
			public void addSelectionComponents(Table innerTable) {
				Image image = new Image(noneSelectedDrawable);
				innerTable.add(image).size(183, 183).pad(10).row();
				addNoneSelectedTooltip(innerTable);
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

	private void addNoneSelectedTooltip(Actor actor) {
		Table tooltipTable = new Table();

		String headerText = i18nTranslator.getTranslatedString("ITEM.NONE_SELECTED").toString();
		tooltipTable.add(new Label(headerText, skin)).center();

		tooltipFactory.complexTooltip(actor, tooltipTable, TooltipFactory.TooltipBackground.LARGE_PATCH_DARK);
	}

	private List<ItemType> getSelectableItemTypes(RoomType currentRoomType) {
		if (furnitureEntity.getBehaviourComponent() instanceof TradingImportFurnitureBehaviour) {
			return Stream.concat(
						craftingRecipeDictionary.getAll().stream()
								.map(CraftingRecipe::getOutput)
								.map(QuantifiedItemTypeWithMaterial::getItemType)
								.filter(Objects::nonNull),
						itemTypeDictionary.getTradeImports().stream()
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
		private final ItemType itemType;

		public SelectItemTypeOption(ItemType itemType, Runnable onSelection) {
			super(I18nText.BLANK);
			this.itemType = itemType;
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
			craftingHintWidgetFactory.addComplexTooltip(innerTable, skin, itemType, null);
		}

		@Override
		public void onSelect() {
			onSelection.run();
			messageDispatcher.dispatchMessage(MessageType.GUI_REMOVE_ALL_TOOLTIPS);
		}
	}

}
