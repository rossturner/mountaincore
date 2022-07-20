package technology.rocketjump.saul.screens;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.crafting.CraftingRecipeDictionary;
import technology.rocketjump.saul.crafting.model.CraftingRecipe;
import technology.rocketjump.saul.crafting.model.CraftingRecipeMaterialSelection;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ExampleItemDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.model.CraftingType;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;
import technology.rocketjump.saul.settlement.ItemTracker;
import technology.rocketjump.saul.settlement.LiquidTracker;
import technology.rocketjump.saul.settlement.SettlerTracker;
import technology.rocketjump.saul.settlement.production.ProductionManager;
import technology.rocketjump.saul.settlement.production.ProductionQuota;
import technology.rocketjump.saul.ui.Scene2DUtils;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nUpdatable;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ImageButton;
import technology.rocketjump.saul.ui.widgets.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.entities.tags.CraftingStationBehaviourTag.CRAFTING_STATION_BEHAVIOUR_TAGNAME;
import static technology.rocketjump.saul.materials.model.GameMaterial.NULL_MATERIAL;

@Singleton
public class CraftingManagementScreen extends ManagementScreen implements I18nUpdatable, Telegraph {

	private static final float INDENT_WIDTH = 50f;
	public static final int DEFAULT_ROW_WIDTH = 1050;
	public static final String NAME = "CRAFTING";

	private final ClickableTableFactory clickableTableFactory;
	private final ExampleItemDictionary exampleItemDictionary;
	private final EntityRenderer entityRenderer;
	private final CraftingRecipeDictionary craftingRecipeDictionary;
	private final RoomTypeDictionary roomTypeDictionary;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final CraftingTypeDictionary craftingTypeDictionary;
	private final ProductionManager productionManager;
	private final SettlerTracker settlerTracker;
	private final GameMaterialDictionary gameMaterialDictionary;

	private boolean initialised = false;
	private final List<CraftingType> displayedCraftingTypes = new ArrayList<>();
	private final Map<CraftingType, List<FurnitureType>> craftingStationsByType = new TreeMap<>();
	private final Map<FurnitureType, List<RoomType>> roomsForCraftingStations = new HashMap<>();
	private final Map<CraftingType, Map<ItemType, List<CraftingRecipe>>> producedItemTypesByCraftingRecipe = new HashMap<>();
	private final Map<CraftingType, Map<GameMaterial, List<CraftingRecipe>>> producedLiquidsByCraftingRecipe = new HashMap<>();

	private final Table scrollableTable;
	private final ScrollPane scrollableTablePane;

	private final Set<CraftingType> hiddenCrafringTypes = new HashSet<>();
	private final Set<CraftingType> expandedCraftingTypes = new HashSet<>();
	private final Set<ItemType> expandedItemTypes = new HashSet<>();
	private final Set<GameMaterial> expandedLiquidMaterials = new HashSet<>();
	private final ItemTracker itemTracker;
	private final LiquidTracker liquidTracker;
	private final List<ToggleButtonSet.ToggleButtonDefinition> buttonDefinitions;

	@Inject
	public CraftingManagementScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
									GuiSkinRepository guiSkinRepository, I18nWidgetFactory i18nWidgetFactory,
									I18nTranslator i18nTranslator, IconButtonFactory iconButtonFactory,
									CraftingTypeDictionary craftingTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
									RoomTypeDictionary roomTypeDictionary, CraftingRecipeDictionary craftingRecipeDictionary,
									ClickableTableFactory clickableTableFactory, ExampleItemDictionary exampleItemDictionary,
									EntityRenderer entityRenderer, ProductionManager productionManager,
									SettlerTracker settlerTracker, ItemTracker itemTracker, LiquidTracker liquidTracker,
									TextureAtlasRepository textureAtlasRepository, GameMaterialDictionary gameMaterialDictionary) {
		super(userPreferences, messageDispatcher, guiSkinRepository, i18nWidgetFactory, i18nTranslator, iconButtonFactory);
		this.clickableTableFactory = clickableTableFactory;
		this.exampleItemDictionary = exampleItemDictionary;
		this.entityRenderer = entityRenderer;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.roomTypeDictionary = roomTypeDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.productionManager = productionManager;
		this.settlerTracker = settlerTracker;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.itemTracker = itemTracker;
		this.liquidTracker = liquidTracker;

		scrollableTable = new Table(uiSkin);
		scrollableTablePane = Scene2DUtils.wrapWithScrollPane(scrollableTable, uiSkin);

		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			displayedCraftingTypes.add(craftingType);
			craftingStationsByType.put(craftingType, new ArrayList<>());
		}

		buttonDefinitions = new ArrayList<>();
		TextureAtlas guiTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		for (JobPriority jobPriority : Arrays.asList(JobPriority.DISABLED, JobPriority.LOWER, JobPriority.NORMAL, JobPriority.HIGHER)) {
			Sprite sprite = guiTextureAtlas.createSprite(jobPriority.iconName);
			sprite.scale(0.5f);
			sprite.setSize(sprite.getWidth() / 2f, sprite.getHeight() / 2f);
			sprite.setColor(jobPriority.color);
			buttonDefinitions.add(new ToggleButtonSet.ToggleButtonDefinition(jobPriority.name(), sprite));
		}

		messageDispatcher.addListener(this, MessageType.SHOW_SPECIFIC_CRAFTING);
	}

	private void initialise() {
		for (FurnitureType furnitureType : furnitureTypeDictionary.getAll()) {
			if (furnitureType.getTags().containsKey(CRAFTING_STATION_BEHAVIOUR_TAGNAME)) {
				String craftingTypeName = furnitureType.getTags().get(CRAFTING_STATION_BEHAVIOUR_TAGNAME).get(0);
				CraftingType craftingType = craftingTypeDictionary.getByName(craftingTypeName);
				if (craftingType != null) {
					craftingStationsByType.get(craftingType).add(furnitureType);

					for (RoomType roomType : roomTypeDictionary.getAll()) {
						if (roomType.getFurnitureNames().contains(furnitureType.getName())) {
							roomsForCraftingStations.computeIfAbsent(furnitureType, a -> new ArrayList<>()).add(roomType);
						}
					}

					Map<ItemType, List<CraftingRecipe>> recipesForProducedItems = new HashMap<>();
					Map<GameMaterial, List<CraftingRecipe>> recipesForProducedLiquids = new HashMap<>();
					for (CraftingRecipe craftingRecipe : craftingRecipeDictionary.getByCraftingType(craftingType)) {
						for (QuantifiedItemTypeWithMaterial recipeOutput : craftingRecipe.getOutput()) {
							if (recipeOutput.isLiquid()) {
								recipesForProducedLiquids.computeIfAbsent(recipeOutput.getMaterial(), a -> new ArrayList<>()).add(craftingRecipe);
							} else {
								recipesForProducedItems.computeIfAbsent(recipeOutput.getItemType(), a -> new ArrayList<>()).add(craftingRecipe);
							}
						}
					}

					producedItemTypesByCraftingRecipe.put(craftingType, recipesForProducedItems);
					producedLiquidsByCraftingRecipe.put(craftingType, recipesForProducedLiquids);
				} else {
					Logger.error("Could not find crafting type with name " + craftingTypeName + " from tag for " + furnitureType.getName() + " in " + this.getClass().getSimpleName());
				}
			}
		}

		onLanguageUpdated();

		initialised = true;
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == MessageType.GUI_SCALE_CHANGED) {
			return super.handleMessage(msg);
		} else {
			switch (msg.message) {
				case MessageType.SHOW_SPECIFIC_CRAFTING: {
					if (!initialised) {
						initialise();
					}
					hiddenCrafringTypes.clear();
					expandedCraftingTypes.clear();
					expandedItemTypes.clear();
					expandedLiquidMaterials.clear();
					CraftingType craftingTypeToShow = (CraftingType) msg.extraInfo;
					for (CraftingType type : craftingTypeDictionary.getAll()) {
						if (type.equals(craftingTypeToShow)) {
							expandedCraftingTypes.add(type);
							expandedItemTypes.addAll(producedItemTypesByCraftingRecipe.get(type).keySet());
							expandedLiquidMaterials.addAll(producedLiquidsByCraftingRecipe.get(type).keySet());
						} else {
							hiddenCrafringTypes.add(type);
						}
					}
					reset();
					return true;
				}
				default:
					throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
			}
		}
	}

	@Override
	public void hide() {
		if (!hiddenCrafringTypes.isEmpty()) {
			hiddenCrafringTypes.clear();
			expandedCraftingTypes.clear();
			expandedItemTypes.clear();
			expandedLiquidMaterials.clear();
		}
	}

	@Override
	public void reset() {
		if (!initialised) {
			initialise();
		}
		containerTable.clearChildren();
		containerTable.add(titleLabel).center().pad(5).row();
		scrollableTable.clearChildren();

		for (CraftingType craftingType : displayedCraftingTypes) {
			if (hiddenCrafringTypes.contains(craftingType)) {
				continue;
			}
			List<FurnitureType> craftingStations = craftingStationsByType.get(craftingType);
			addCraftingTypeRow(craftingType, craftingStations);

			boolean craftingTypeExpanded = expandedCraftingTypes.contains(craftingType);

			if (craftingTypeExpanded) {
				for (Map.Entry<ItemType, List<CraftingRecipe>> producedItemEntry : producedItemTypesByCraftingRecipe.get(craftingType).entrySet()) {
					addProducedItemRow(new CraftingOutput(producedItemEntry.getKey()));

					if (expandedItemTypes.contains(producedItemEntry.getKey())) {
						for (CraftingRecipe craftingRecipe : producedItemEntry.getValue()) {
							addCraftingRecipeRow(craftingRecipe);
						}
					}
				}
				for (Map.Entry<GameMaterial, List<CraftingRecipe>> producedLiquidEntry : producedLiquidsByCraftingRecipe.get(craftingType).entrySet()) {
					addProducedItemRow(new CraftingOutput(producedLiquidEntry.getKey()));

					if (expandedLiquidMaterials.contains(producedLiquidEntry.getKey())) {
						for (CraftingRecipe craftingRecipe : producedLiquidEntry.getValue()) {
							addCraftingRecipeRow(craftingRecipe);
						}
					}
				}
			}
		}


		containerTable.add(scrollableTablePane).pad(2);
	}

	private void addCraftingTypeRow(CraftingType craftingType, List<FurnitureType> craftingStations) {

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		clickableRow.setAction(() -> {
			if (expandedCraftingTypes.contains(craftingType)) {
				expandedCraftingTypes.remove(craftingType);
			} else {
				expandedCraftingTypes.add(craftingType);
				// Also remove all expanded children
				for (ItemType childItemType : producedItemTypesByCraftingRecipe.get(craftingType).keySet()) {
					expandedItemTypes.remove(childItemType);
				}
				for (GameMaterial childLiquidMaterial : producedLiquidsByCraftingRecipe.get(craftingType).keySet()) {
					expandedLiquidMaterials.remove(childLiquidMaterial);
				}
			}
			reset();
		});


		if (craftingType.getProfessionRequired() != null) {
			ImageButton imageButton = craftingType.getProfessionRequired().getImageButton();
			clickableRow.add(new Image(imageButton.getDrawable())).left().padLeft(10);
		}
		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedString(craftingType.getI18nKey()), uiSkin, messageDispatcher)).left().pad(5).padLeft(10);

		String collectedCraftingStationText = craftingStations.stream()
				.map(craftingStation -> {
					String craftingStationName = i18nTranslator.getTranslatedString(craftingStation.getI18nKey()).toString();

					List<RoomType> roomTypes = roomsForCraftingStations.get(craftingStation);
					String roomTypesString = roomTypes.stream().map(roomType -> i18nTranslator.getTranslatedString(roomType.getI18nKey()).toString()).collect(Collectors.joining(", "));
					return craftingStationName + " (" + roomTypesString + ")";
				})
				.collect(Collectors.joining(", "));
		clickableRow.add(new Label(collectedCraftingStationText, uiSkin)).right().expandX().pad(5);

		scrollableTable.add(clickableRow).center().width(DEFAULT_ROW_WIDTH).height(64).row();
	}

	public static class CraftingOutput {

		public final boolean isLiquid;
		public final GameMaterial liquidMaterial;
		public final ItemType itemType;

		public CraftingOutput(GameMaterial liquidMaterial) {
			this.isLiquid = true;
			this.liquidMaterial = liquidMaterial;
			this.itemType = null;
		}

		public CraftingOutput(ItemType itemType) {
			this.isLiquid = false;
			this.liquidMaterial = null;
			this.itemType = itemType;
		}

	}

	private void addProducedItemRow(CraftingOutput craftingOutput) {
		Table rowContainerTable = new Table(uiSkin);
		rowContainerTable.add(new Container<>()).width(1 * INDENT_WIDTH);
		ProductionQuota currentProductionQuota;
		if (craftingOutput.isLiquid) {
			currentProductionQuota = productionManager.getProductionQuota(craftingOutput.liquidMaterial);
		} else {
			currentProductionQuota = productionManager.getProductionQuota(craftingOutput.itemType);
		}

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		clickableRow.setFillParent(true);
		clickableRow.setAction(() -> {
			if (craftingOutput.isLiquid) {
				if (expandedLiquidMaterials.contains(craftingOutput.liquidMaterial)) {
					expandedLiquidMaterials.remove(craftingOutput.liquidMaterial);
				} else {
					expandedLiquidMaterials.add(craftingOutput.liquidMaterial);
				}
			} else {
				if (expandedItemTypes.contains(craftingOutput.itemType)) {
					expandedItemTypes.remove(craftingOutput.itemType);
				} else {
					expandedItemTypes.add(craftingOutput.itemType);
				}
			}
			reset();
		});

		EntityDrawable materialDrawable = new EntityDrawable(getExampleEntity(craftingOutput.itemType, craftingOutput.liquidMaterial), entityRenderer);
		clickableRow.add(new Image(materialDrawable)).left().width(80).pad(5);

		String i18nKey = craftingOutput.isLiquid ? craftingOutput.liquidMaterial.getI18nKey() : craftingOutput.itemType.getI18nKey();
		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedString(i18nKey), uiSkin, messageDispatcher)).left().pad(10);

		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedString("GUI.CRAFTING_MANAGEMENT.MAINTAINING_TEXT"), uiSkin, messageDispatcher)).pad(5);

		TextField quantityInput = new TextField("0", uiSkin);
		if (currentProductionQuota.isFixedAmount()) {
			quantityInput.setText(String.valueOf(currentProductionQuota.getFixedAmount()));
		} else {
			quantityInput.setText(String.valueOf(currentProductionQuota.getPerSettler()));
		}
		quantityInput.setTextFieldFilter(new DigitFilter());
		quantityInput.setAlignment(Align.center);
		clickableRow.add(quantityInput).width(70);

		SelectBox<QuotaSetting> quotaSettingSelect = new SelectBox<>(uiSkin);
		Array<QuotaSetting> settingList = new Array<>();
		settingList.add(QuotaSetting.FIXED_AMOUNT);
		settingList.add(QuotaSetting.PER_SETTLER);
		quotaSettingSelect.setItems(settingList);
		if (currentProductionQuota.isFixedAmount()) {
			quotaSettingSelect.setSelected(QuotaSetting.FIXED_AMOUNT);
		} else {
			quotaSettingSelect.setSelected(QuotaSetting.PER_SETTLER);
		}
		clickableRow.add(quotaSettingSelect);

		I18nTextWidget perSettlerTotalAmountHint = new I18nTextWidget(null, uiSkin, messageDispatcher);

		quantityInput.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				updateQuota(craftingOutput, getInputQuantityValue(quantityInput), quotaSettingSelect.getSelected(), perSettlerTotalAmountHint);
			}
		});
		quotaSettingSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				updateQuota(craftingOutput, getInputQuantityValue(quantityInput), quotaSettingSelect.getSelected(), perSettlerTotalAmountHint);
			}
		});

		clickableRow.add(new Container<>()).pad(5);


		updateHint(perSettlerTotalAmountHint, currentProductionQuota);
		clickableRow.add(perSettlerTotalAmountHint);

		I18nText actualAmountText = i18nTranslator.getTranslatedWordWithReplacements("GUI.CRAFTING_MANAGEMENT.TOTAL_QUANTITY_ACTUAL",
				Map.of("quantity", new I18nWord(String.valueOf(count(craftingOutput)))));
		clickableRow.add(new I18nTextWidget(actualAmountText, uiSkin, messageDispatcher)).right().expandX().padRight(100);

		rowContainerTable.add(clickableRow).width(DEFAULT_ROW_WIDTH - INDENT_WIDTH);
		scrollableTable.add(rowContainerTable).width(DEFAULT_ROW_WIDTH).right().row();
	}

	private Entity getExampleEntity(ItemType itemType, GameMaterial material) {
		return exampleItemDictionary.getExampleItemEntity(itemType, Optional.ofNullable(material));
	}

	private void addCraftingRecipeRow(CraftingRecipe craftingRecipe) {
		Table rowContainerTable = new Table(uiSkin);
		rowContainerTable.add(new Container<>()).width(2 * INDENT_WIDTH);

		ClickableTable clickableRow = clickableTableFactory.create();
		clickableRow.setBackground("default-rect");
		clickableRow.pad(2);
		clickableRow.setFillParent(true);
		clickableRow.setAction(() -> {
			// Do nothing currently
		});


		for (int inputCursor = 0; inputCursor < craftingRecipe.getInput().size(); inputCursor++) {
			QuantifiedItemTypeWithMaterial inputRequirement = craftingRecipe.getInput().get(inputCursor);
			EntityDrawable itemDrawable = new EntityDrawable(getExampleEntity(inputRequirement.getItemType(), inputRequirement.getMaterial()), entityRenderer);
			clickableRow.add(new Image(itemDrawable)).left().pad(5);
			I18nText description = i18nTranslator.getItemDescription(inputRequirement.getQuantity(), inputRequirement.getMaterial(),
					inputRequirement.getItemType(), null);
			I18nTextWidget descriptionWidget = new I18nTextWidget(description, uiSkin, messageDispatcher);

			VerticalGroup descriptionGroup = new VerticalGroup();
			descriptionGroup.padBottom(5);
			descriptionGroup.addActor(descriptionWidget);

			if (inputRequirement.getMaterial() == null && !inputRequirement.isLiquid()) {
				descriptionGroup.addActor(buildMaterialSelect(inputRequirement, craftingRecipe));
			}

			clickableRow.add(descriptionGroup).pad(5);

			if (inputCursor < craftingRecipe.getInput().size() - 1) {
				// add + for next input
				clickableRow.add(new Label("+", uiSkin)).pad(5);
			}
		}

		clickableRow.add(new Label("=>", uiSkin)).pad(5);

		for (int outputCursor = 0; outputCursor < craftingRecipe.getOutput().size(); outputCursor++) {
			QuantifiedItemTypeWithMaterial outputRequirement = craftingRecipe.getOutput().get(outputCursor);
			EntityDrawable entityDrawable = new EntityDrawable(getExampleEntity(outputRequirement.getItemType(), outputRequirement.getMaterial()), entityRenderer);
			clickableRow.add(new Image(entityDrawable)).left().pad(5);
			I18nText description = i18nTranslator.getItemDescription(outputRequirement.getQuantity(), outputRequirement.getMaterial(),
					outputRequirement.getItemType(), null);
			clickableRow.add(new I18nTextWidget(description, uiSkin, messageDispatcher)).pad(5);

			if (outputCursor < craftingRecipe.getOutput().size() - 1) {
				// add + for next input
				clickableRow.add(new Label("+", uiSkin)).pad(5);
			}
		}

		ToggleButtonSet priorityToggle = new ToggleButtonSet(uiSkin, buttonDefinitions, (value) -> {
			JobPriority selectedPriority = JobPriority.valueOf(value);
			productionManager.setRecipePriority(craftingRecipe, selectedPriority);
		});
		priorityToggle.setChecked(productionManager.getRecipePriority(craftingRecipe).name());

		VerticalGroup priorityStack = new VerticalGroup();
		priorityStack.addActor(new Label(i18nTranslator.getTranslatedString("GUI.PRIORITY_LABEL").toString(), uiSkin));
		priorityStack.addActor(priorityToggle);
		priorityStack.align(Align.left);
		priorityStack.pad(2);

		clickableRow.add(priorityStack).right().pad(10).padRight(INDENT_WIDTH * 3).expandX();

		rowContainerTable.add(clickableRow).width(DEFAULT_ROW_WIDTH - (INDENT_WIDTH * 2));
		scrollableTable.add(rowContainerTable).width(DEFAULT_ROW_WIDTH).right().row();
	}

	private SelectBox<MaterialSelectOption> buildMaterialSelect(QuantifiedItemTypeWithMaterial inputRequirement, CraftingRecipe craftingRecipe) {
		final CraftingRecipeMaterialSelection craftingRecipeMaterialSelection = gameContext.getSettlementState()
				.craftingRecipeMaterialSelections.computeIfAbsent(craftingRecipe, a -> new CraftingRecipeMaterialSelection(craftingRecipe));
		Optional<GameMaterial> selectedMaterial = craftingRecipeMaterialSelection.getSelection(inputRequirement);

		SelectBox<MaterialSelectOption> materialSelect = new SelectBox<>(uiSkin);

		List<MaterialSelectOption> options = new ArrayList<>();

		List<GameMaterial> materialsToTry = new ArrayList<>();
		materialsToTry.add(NULL_MATERIAL);
		materialsToTry.addAll(gameMaterialDictionary.getByType(inputRequirement.getItemType().getPrimaryMaterialType()));

		for (GameMaterial material : materialsToTry) {
			StringBuilder labelBuilder = new StringBuilder();

			Collection<Entity> unallocatedItems;
			if (NULL_MATERIAL.equals(material)) {
				labelBuilder.append(i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString()).append(" (");
				unallocatedItems = itemTracker.getItemsByType(inputRequirement.getItemType(), true);
			} else {
				labelBuilder.append(material.getI18nValue()).append(" (");
				unallocatedItems = itemTracker.getItemsByTypeAndMaterial(inputRequirement.getItemType(), material, true);
				if (unallocatedItems.isEmpty() && !(selectedMaterial.orElse(NULL_MATERIAL).equals(material))) {
					continue;
				}
			}

			int unallocatedQuantity = 0;
			for (Entity itemEntity : unallocatedItems) {
				ItemAllocationComponent itemAllocationComponent = itemEntity.getComponent(ItemAllocationComponent.class);
				if (itemAllocationComponent != null) {
					unallocatedQuantity += itemAllocationComponent.getNumUnallocated();
				}
			}

			labelBuilder.append(unallocatedQuantity).append(")");

			options.add(new MaterialSelectOption(labelBuilder.toString(), unallocatedQuantity, material));
		}

		Collections.sort(options);

		Array<MaterialSelectOption> optionsArray = new Array<>();
		for (MaterialSelectOption option : options) {
			optionsArray.add(option);
		}
		materialSelect.setItems(optionsArray);

		if (selectedMaterial.isEmpty()) {
			materialSelect.setSelected(optionsArray.first());
		} else {
			materialSelect.setSelected(options.stream().filter(a -> a.value.equals(selectedMaterial.get())).findFirst().orElse(optionsArray.first()));
		}

		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				MaterialSelectOption selected = materialSelect.getSelected();
				CraftingRecipeMaterialSelection materialSelection = gameContext.getSettlementState().craftingRecipeMaterialSelections.computeIfAbsent(craftingRecipe, a -> new CraftingRecipeMaterialSelection(craftingRecipe));
				materialSelection.setSelection(inputRequirement, selected.value);
			}
		});

		return materialSelect;
	}

	private static class MaterialSelectOption implements Comparable<MaterialSelectOption> {

		public final String label;
		public final int quantity;
		public final GameMaterial value;

		public MaterialSelectOption(String label, int quantity, GameMaterial value) {
			this.label = label;
			this.quantity = quantity;
			this.value = value;
		}

		@Override
		public String toString() {
			return label;
		}

		@Override
		public int compareTo(MaterialSelectOption o) {
			return o.quantity - this.quantity;
		}
	}

	private void updateQuota(CraftingOutput craftingOutput, float quantity, QuotaSetting quotaSetting, I18nTextWidget perSettlerTotalAmountHint) {
		ProductionQuota quota = new ProductionQuota();
		if (quotaSetting.equals(QuotaSetting.FIXED_AMOUNT)) {
			quota.setFixedAmount((int)quantity);
		} else {
			quota.setPerSettler(quantity);
		}

		if (craftingOutput.isLiquid) {
			productionManager.productionQuoteModified(craftingOutput.liquidMaterial, quota);
		} else {
			productionManager.productionQuoteModified(craftingOutput.itemType, quota);
		}

		updateHint(perSettlerTotalAmountHint, quota);
	}

	private void updateHint(I18nTextWidget perSettlerTotalAmountHint, ProductionQuota productionQuota) {
		int requiredAmount = productionQuota.getRequiredAmount(settlerTracker.getLiving().size());
		if (productionQuota.isFixedAmount() && requiredAmount > 0) {
			perSettlerTotalAmountHint.setVisible(false);
		} else {
			perSettlerTotalAmountHint.setError(requiredAmount == 0);
			I18nText actualAmountText = i18nTranslator.getTranslatedWordWithReplacements("GUI.CRAFTING_MANAGEMENT.TOTAL_QUANTITY_HINT",
					Map.of("quantity", new I18nWord(String.valueOf(requiredAmount))));
			perSettlerTotalAmountHint.setI18nText(actualAmountText);
			perSettlerTotalAmountHint.setVisible(true);

		}
	}

	public float getInputQuantityValue(TextField quantityInput) {
		String text = quantityInput.getText();
		float asFloat = 0f;
		try {
			asFloat = Float.valueOf(text);
		} catch (NumberFormatException e) {
			quantityInput.setText("0");
		}
		return asFloat;
	}

	private int count(CraftingOutput craftingOutput) {
		if (craftingOutput.isLiquid) {
			return (int) liquidTracker.getCurrentLiquidAmount(craftingOutput.liquidMaterial);
		} else {
			return itemTracker.getItemsByType(craftingOutput.itemType, false).stream()
					.map(entity -> ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getQuantity())
					.reduce(0, Integer::sum);
		}
	}


	@Override
	public String getTitleI18nKey() {
		return "GUI.CRAFTING_MANAGEMENT.TITLE";
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {

	}

	@Override
	public void clearContextRelatedState() {
	}

	@Override
	public void onLanguageUpdated() {
		displayedCraftingTypes.sort(Comparator.comparing(a -> i18nTranslator.getTranslatedString(a.getI18nKey()).toString()));

		QuotaSetting.FIXED_AMOUNT.i18nValue = i18nTranslator.getTranslatedString("QUOTA.FIXED_AMOUNT").toString();
		QuotaSetting.PER_SETTLER.i18nValue = i18nTranslator.getTranslatedString("QUOTA.PER_SETTLER").toString();
	}

	public enum QuotaSetting {

		FIXED_AMOUNT,
		PER_SETTLER;

		public String i18nValue;


		@Override
		public String toString() {
			return i18nValue == null ? this.name() : i18nValue;
		}
	}

	public static class DigitFilter implements TextField.TextFieldFilter {

		private char[] accepted;

		public DigitFilter() {
			accepted = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'};
		}

		@Override
		public boolean acceptChar(TextField textField, char c) {
			for (char a : accepted)
				if (a == c) return true;
			return false;
		}
	}
}
