package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.screens.SettlerManagementScreen;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.Updatable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;
import technology.rocketjump.saul.ui.skins.ManagementSkin;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.*;
import technology.rocketjump.saul.ui.widgets.text.DecoratedString;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabel;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabelFactory;

import java.util.List;
import java.util.*;
import java.util.function.Function;

import static technology.rocketjump.saul.ui.Selectable.SelectableType.ENTITY;

@Singleton
public class EntitySelectedGuiView implements GuiView, GameContextAware {

	private final SoundAssetDictionary soundAssetDictionary;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
//	private final IconButton viewCraftingButton;
//	private final IconButton deconstructButton;
//	private final IconButton emptyLiquidContainerButton;
	private final EntityStore entityStore;
	private final JobStore jobStore;
	private final TooltipFactory tooltipFactory;
	private final MessageDispatcher messageDispatcher;
	private final MainGameSkin mainGameSkin;
	private final ManagementSkin managementSkin;
	private final MenuSkin menuSkin;
	private final DecoratedStringLabelFactory decoratedStringLabelFactory;
	//	private final JobType haulingJobType;
//	private final ImageButton changeSettlerNameButton;
//	private final ClickableTable squadTextButton;

	private Table outerTable;
	private List<Updatable<?>> updatables;

//	private Table entityDescriptionTable;
	private GameContext gameContext;
//	private Label beingDeconstructedLabel;
//	private I18nCheckbox militaryToggleCheckbox;
//	private Selectable previousSelectable;

//	private final Table nameTable;
//	private final Table happinessTable;
//	private final Table injuriesTable;
//	private final Table inventoryTable;
//
//	private final Table upperRow;
//	private final Table lowerRow;
//
//	private final I18nLabel inventoryLabel;

	//TODO: remove me asap
	private final SettlerManagementScreen settlerManagementScreen;


	// For stockpiles
	private FurnitureStockpileComponent currentStockpileComponent;
	private StockpileManagementTree stockpileManagementTree;
	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final RaceDictionary raceDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private List<ToggleButtonSet.ToggleButtonDefinition> priorityButtonDefinitions;


	@Inject
	public EntitySelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
	                             GameInteractionStateContainer gameInteractionStateContainer,
	                             EntityStore entityStore, JobStore jobStore,
	                             I18nWidgetFactory i18nWidgetFactory, JobTypeDictionary jobTypeDictionary,
	                             TooltipFactory tooltipFactory, DecoratedStringLabelFactory decoratedStringLabelFactory,
	                             StockpileComponentUpdater stockpileComponentUpdater, StockpileGroupDictionary stockpileGroupDictionary,
	                             GameMaterialDictionary gameMaterialDictionary, RaceDictionary raceDictionary,
	                             ItemTypeDictionary itemTypeDictionary, SoundAssetDictionary soundAssetDictionary, SettlerManagementScreen settlerManagementScreen) {
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.entityStore = entityStore;
		this.jobStore = jobStore;
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		this.decoratedStringLabelFactory = decoratedStringLabelFactory;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.raceDictionary = raceDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
/*
		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		entityDescriptionTable = new Table(uiSkin);
		entityDescriptionTable.pad(10);

		haulingJobType = jobTypeDictionary.getByName("HAULING");


		viewCraftingButton = iconButtonFactory.create("GUI.CRAFTING_MANAGEMENT.TITLE", "gears", HexColors.get("#6677FF"), ButtonStyle.SMALL);
		viewCraftingButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(ENTITY)) {
				BehaviourComponent behaviourComponent = selectable.getEntity().getBehaviourComponent();
				if (behaviourComponent instanceof CraftingStationBehaviour) {
					messageDispatcher.dispatchMessage(MessageType.SHOW_SPECIFIC_CRAFTING, ((CraftingStationBehaviour) behaviourComponent).getCraftingType());
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, ManagementScreenName.CRAFTING.name());
				}
			}
		});

		for (int i = 0; i <= 4; i++) {
			cancelButtons.add(imageButtonFactory.getOrCreate("cancel", true).clone());
			upButtons.add(iconButtonFactory.create("arrow-up").scale(0.5f));
			downButtons.add(iconButtonFactory.create("arrow-down").scale(0.5f));
		}

		deconstructButton = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.SMALL);
		deconstructButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(ENTITY)) {
				Entity entity = selectable.getEntity();
				ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
				if (constructedEntityComponent != null && !constructedEntityComponent.isBeingDeconstructed()) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, selectable.getEntity());
					update();
				}
			}
		});

		emptyLiquidContainerButton = iconButtonFactory.create("GUI.EMPTY_CONTAINER_LABEL", "cardboard-box", HexColors.get("#f4ec78"), ButtonStyle.SMALL);
		emptyLiquidContainerButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (isItemContainingLiquidOnGroundAndNoneAllocated(selectable.getEntity())) {
				Entity entity = selectable.getEntity();
				messageDispatcher.dispatchMessage(MessageType.REQUEST_DUMP_LIQUID_CONTENTS, entity);
				update();
			}
		});

		beingDeconstructedLabel = i18nWidgetFactory.createLabel("GUI.FURNITURE_BEING_REMOVED");
		inventoryLabel = i18nWidgetFactory.createLabel("INVENTORY.CONTAINS.LABEL");

		militaryToggleCheckbox = new I18nCheckbox("CIVILIAN", "civilian", uiSkin);
		militaryToggleCheckbox.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				Selectable selectable = gameInteractionStateContainer.getSelectable();
				if (selectable.getEntity() != null) {
					Entity entity = selectable.getEntity();
					MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
					boolean checked = militaryToggleCheckbox.isChecked();
					if (checked) {
						militaryComponent.addToMilitary(1L);
					} else {
						militaryComponent.removeFromMilitary();
					}
					update();
				}
			}
		});

		nameTable = new Table(uiSkin);
		happinessTable = new Table(uiSkin);
		injuriesTable = new Table(uiSkin);
		inventoryTable = new Table(uiSkin);
		militaryToggleTable = new Table(uiSkin);

		upperRow = new Table(uiSkin);
		lowerRow = new Table(uiSkin);

		needLabels = i18nWidgetFactory.createNeedsLabels();

		UNARMED_IMAGE_BUTTON = imageButtonFactory.getOrCreate("punch");
		UNSHIELDED_IMAGE_BUTTON = imageButtonFactory.getOrCreate("chicken-oven");
		UNARMORED_IMAGE_BUTTON = imageButtonFactory.getOrCreate("dwarf-face");
		changeSettlerNameButton = imageButtonFactory.getOrCreate("fountain-pen", true).clone();


		squadTextButton = clickableTableFactory.create();
		squadTextButton.setBackground(uiSkin.get(TextButton.TextButtonStyle.class).up);
		squadTextButton.add(new Label("REPLACE ME", uiSkin));

		weaponSelectionAction = () -> {
			messageDispatcher.dispatchMessage(MessageType.PREPOPULATE_SELECT_ITEM_VIEW, new PopulateSelectItemViewMessage(
					PopulateSelectItemViewMessage.ItemSelectionCategory.WEAPON, gameInteractionStateContainer.getSelectable().getEntity(),
					entity -> {
						Entity settler = gameInteractionStateContainer.getSelectable().getEntity();
						if (settler != null) {
							MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
							if (entity != null) {
								militaryComponent.setAssignedWeaponId(entity.getId());
								if (isTwoHandedWeapon(entity)) {
									militaryComponent.setAssignedShieldId(null);
								}
							} else {
								militaryComponent.setAssignedWeaponId(null);
							}
							militaryComponent.infrequentUpdate(0.0);
						}
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, this.getName());
					}));
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.SELECT_ITEM);
		};
		shieldSelectionAction = () -> {
			messageDispatcher.dispatchMessage(MessageType.PREPOPULATE_SELECT_ITEM_VIEW, new PopulateSelectItemViewMessage(
					PopulateSelectItemViewMessage.ItemSelectionCategory.SHIELD, gameInteractionStateContainer.getSelectable().getEntity(),
					entity -> {
						Entity settler = gameInteractionStateContainer.getSelectable().getEntity();
						if (settler != null) {
							MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
							if (entity != null) {
								militaryComponent.setAssignedShieldId(entity.getId());
							} else {
								militaryComponent.setAssignedShieldId(null);
							}
							militaryComponent.infrequentUpdate(0.0);
						}
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, this.getName());
					}));
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.SELECT_ITEM);
		};
		armorSelectionAction = () -> {
			messageDispatcher.dispatchMessage(MessageType.PREPOPULATE_SELECT_ITEM_VIEW, new PopulateSelectItemViewMessage(
					PopulateSelectItemViewMessage.ItemSelectionCategory.ARMOR, gameInteractionStateContainer.getSelectable().getEntity(),
					entity -> {
						Entity settler = gameInteractionStateContainer.getSelectable().getEntity();
						if (settler != null) {
							MilitaryComponent militaryComponent = settler.getComponent(MilitaryComponent.class);
							if (entity != null) {
								militaryComponent.setAssignedArmorId(entity.getId());
							} else {
								militaryComponent.setAssignedArmorId(null);
							}
							militaryComponent.infrequentUpdate(0.0);
						}
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, this.getName());
					}));
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.SELECT_ITEM);
		};


		priorityButtonDefinitions = new ArrayList<>();
		// TODO might want to pull the below code out somewhere else
		TextureAtlas guiTextureAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);
		for (JobPriority jobPriority : Arrays.asList(JobPriority.LOWEST, JobPriority.LOWER, JobPriority.NORMAL, JobPriority.HIGHER, JobPriority.HIGHEST)) {
			Sprite sprite = guiTextureAtlas.createSprite(jobPriority.iconName);
			sprite.scale(0.5f);
			sprite.setSize(sprite.getWidth() / 2f, sprite.getHeight() / 2f);
			sprite.setColor(jobPriority.color);
			priorityButtonDefinitions.add(new ToggleButtonSet.ToggleButtonDefinition(jobPriority.name(), sprite));
		}*/
		this.settlerManagementScreen = settlerManagementScreen;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();
		updatables = new ArrayList<>();
		outerTable = new Table();
		outerTable.setBackground(mainGameSkin.getDrawable("asset_dwarf_select_bg"));
		float dropshadowLength = 18f;
		containerTable.add(outerTable).padLeft(dropshadowLength); //Value of drop shadow on bottom for equal distance

//		outerTable.debug();

		Selectable selectable = gameInteractionStateContainer.getSelectable();
		if (selectable != null && ENTITY == selectable.type) {
			Entity entity = selectable.getEntity();
			if (entity.isSettler()) {

				boolean isMilitary = SettlerManagementScreen.IS_MILITARY.test(entity);
				Updatable<Table> settlerName = creatureName(entity);
				Updatable<Table> happinessIcons = happinessIcons(entity);
				Updatable<Table> textSummary = textSummary(entity);
				Table militaryToggle = settlerManagementScreen.militaryToggle(entity, false, s -> populate(containerTable));
				Table weaponSelection = settlerManagementScreen.weaponSelection(entity, 0.8f, s -> populate(containerTable));
				Table professionSelection = settlerManagementScreen.professions(entity, 0.8f, s -> update());
				Updatable<Table> needs = settlerManagementScreen.needs(entity);
				updatables.add(settlerName);
				updatables.add(happinessIcons);
				updatables.add(textSummary);
				updatables.add(needs);

				//Top left first row - name and toggle
				Table topLeftFirstRow = new Table();
				topLeftFirstRow.add(settlerName.getActor()).spaceRight(36f);
				topLeftFirstRow.add(militaryToggle).spaceRight(86f);

				//Top left second row - Happiness and status for Civ / Squad for military
				Table topLeftSecondRow = new Table();

				if (isMilitary) {
				} else {
					topLeftSecondRow.add(happinessIcons.getActor()).left();
					topLeftSecondRow.add(textSummary.getActor()).left().spaceLeft(25f).top();
				}


				//Top Left Column - 2 rows
				Table topLeftColumn = new Table();
				topLeftColumn.add(topLeftFirstRow).spaceBottom(35f).row();
				topLeftColumn.add(topLeftSecondRow).left().top().expandY();

				//Top Row - 2 Cols
				Table topRow = new Table();
				topRow.columnDefaults(0).spaceLeft(64f);//.spaceTop(43f);
				topRow.columnDefaults(1).spaceRight(60f);//.spaceTop(51f);

				topRow.add(topLeftColumn).top();
				if (isMilitary) {
					topRow.add(weaponSelection);
				} else {
					topRow.add(professionSelection);
				}

				//Bottom Row
				Table bottomRow = new Table();
				bottomRow.add(needs.getActor());
//				bottomRow.add(inventory) //TODO: inventory aligned left, colspan rest

//				outerTable.columnDefaults(0).padLeft(64).left();
				outerTable.add(topRow).left().row();
				outerTable.add(bottomRow).left().padBottom(dropshadowLength);
//				outerTable.add(topRow).left().row();
//				outerTable.add(bottomRow).left();

//				topRow.debug();
//				topLeftFirstRow.debug();
//				topLeftSecondRow.debug();

			} else {
				EntityType entityType = entity.getType();

			}
		}
	}


	/**
	 * Updates every second or so, not instant on show
	 */
	@Override
	public void update() {
		for (Updatable<?> updatable : updatables) {
			updatable.update();
		}

/*
			Entity entity = selectable.getEntity();
			if (entity.isSettler()) {
				buildSettlerSelectedView(entity);
				// TODO description of any dead creatures
			} else {
				entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(entity), uiSkin, messageDispatcher)).left().row();

				for (EntityComponent component : entity.getAllComponents()) {
					if (component instanceof SelectableDescription) {
						for (I18nText description : ((SelectableDescription) component).getDescription(i18nTranslator, gameContext, messageDispatcher)) {
							if (!description.isEmpty()) {
								entityDescriptionTable.add(new I18nTextWidget(description, uiSkin, messageDispatcher)).left().row();
							}
						}
					}
				}

				if (entity.getType().equals(CREATURE)) {
					populateInjuriesTable(entity);
				} else {
					injuriesTable.clearChildren();
				}


				InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
				LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
				if (containsSomething(inventoryComponent, liquidContainerComponent)) {
					entityDescriptionTable.add(inventoryLabel).left().row();
					if (inventoryComponent != null) {
						for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
							entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(inventoryEntry.entity), uiSkin, messageDispatcher)).left().row();
						}
					}
					if (liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0) {
						for (I18nText descriptionString : liquidContainerComponent.i18nDescription(i18nTranslator)) {
							entityDescriptionTable.add(new I18nTextWidget(descriptionString, uiSkin, messageDispatcher)).left().row();
						}
					}
				}

				if (entity.getType().equals(EntityType.FURNITURE) && entity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
					FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (furnitureEntityAttributes.getAssignedToEntityId() != null) {
						Entity assignedToEntity = entityStore.getById(furnitureEntityAttributes.getAssignedToEntityId());
						if (assignedToEntity == null) {
							Logger.error("Could not find furniture's assignedTo entity by ID " + furnitureEntityAttributes.getAssignedToEntityId());
						} else {
							entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getAssignedToLabel(assignedToEntity), uiSkin, messageDispatcher)).left().row();
						}
					}
					if (furnitureEntityAttributes.isDestroyed()) {
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString(furnitureEntityAttributes.getDestructionCause().i18nKey),
								uiSkin, messageDispatcher)).left().row();
					}
				}

				if (entity.getType().equals(EntityType.PLANT)) {
					PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.isAfflictedByPests()) {
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString("CROP.AFFLICTED_BY_PESTS"), uiSkin, messageDispatcher)).left().row();
					}
					if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.CROP)) {
						float harvestProgress = 100f * attributes.estimatedProgressToHarvesting();
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getHarvestProgress(harvestProgress), uiSkin, messageDispatcher)).left().row();
					}
				}

				if (entity.getType().equals(ITEM)) {
					Map<I18nText, Integer> haulingCounts = getHaulingTargetDescriptions(entity);
					for (Map.Entry<I18nText, Integer> targetDescriptionEntry : haulingCounts.entrySet()) {
						Map<String, I18nString> replacements = new HashMap<>();
						replacements.put("targetDescription", targetDescriptionEntry.getKey());
						replacements.put("quantity", new I18nWord(String.valueOf(targetDescriptionEntry.getValue())));
						I18nText allocationDescription = i18nTranslator.getTranslatedWordWithReplacements("HAULING.ASSIGNMENT.DESCRIPTION", replacements);
						entityDescriptionTable.add(new I18nTextWidget(allocationDescription, uiSkin, messageDispatcher)).left().row();
					}

					ItemEntityAttributes itemEntityAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (itemEntityAttributes.isDestroyed()) {
						entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString(itemEntityAttributes.getDestructionCause().i18nKey),
								uiSkin, messageDispatcher)).left().row();
					}

				}

				if (GlobalSettings.DEV_MODE) {

					ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent != null) {
						List<ItemAllocation> itemAllocations = itemAllocationComponent.getAll();
						if (itemAllocations.size() > 0) {
							String allocationsString = StringUtils.join(itemAllocations, ", ");
							entityDescriptionTable.add(new Label("Allocations: " + allocationsString, uiSkin)).left().row();
						}
					}
					if (entity.getType().equals(EntityType.CREATURE)) {
						CombatStateComponent combatStateComponent = entity.getComponent(CombatStateComponent.class);
						if (combatStateComponent != null && combatStateComponent.isInCombat()) {
							if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
								CombatAction currentAction = creatureBehaviour.getCombatBehaviour().getCurrentAction();
								String combatText;
								if (currentAction == null) {
									combatText = "In combat";
								} else {
									combatText = "In combat: " + currentAction.getClass().getSimpleName();
								}
								entityDescriptionTable.add(new Label(combatText, uiSkin)).left().row();
							}
						}
					}
				}
			}

			entityDescriptionTable.row();

			outerTable.add(entityDescriptionTable).top();

			if (injuriesTable.hasChildren() && !entity.isSettler()) {
				outerTable.add(injuriesTable).padLeft(5);
			}

			if (entity.getBehaviourComponent() != null && entity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
				outerTable.add(viewCraftingButton);
			}

			ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
			if (constructedEntityComponent != null) {
				if (constructedEntityComponent.isBeingDeconstructed()) {
					outerTable.add(beingDeconstructedLabel);
				} else if (constructedEntityComponent.canBeDeconstructed()) {
					outerTable.add(deconstructButton);
				}
			}

			if (isItemContainingLiquidOnGroundAndNoneAllocated(entity)) {
				outerTable.add(emptyLiquidContainerButton);
			}

			ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent != null && itemAllocationComponent.getAllocationForPurpose(CONTENTS_TO_BE_DUMPED) != null) {
				outerTable.add(i18nWidgetFactory.createLabel("GUI.EMPTY_CONTAINER_LABEL.BEING_ACTIONED"));
			}

			this.currentStockpileComponent = entity.getComponent(FurnitureStockpileComponent.class);
			if (currentStockpileComponent != null) {
				outerTable.row();

				ToggleButtonSet priorityToggle = new ToggleButtonSet(uiSkin, priorityButtonDefinitions, (value) -> {
					JobPriority selectedPriority = JobPriority.valueOf(value);
					currentStockpileComponent.setPriority(selectedPriority);
				});
				priorityToggle.setChecked(currentStockpileComponent.getPriority().name());

				Table priorityStack = new Table();
				priorityStack.add(new Label(i18nTranslator.getTranslatedString("GUI.PRIORITY_LABEL").toString(), uiSkin)).left().row();
				priorityStack.add(priorityToggle).left().row();
				priorityStack.align(Align.left);
				priorityStack.pad(2);

				outerTable.add(priorityStack).colspan(3).left().row();


				//Dirty hack for now
				if (!sameSelectable) {
					this.stockpileManagementTree = new StockpileManagementTree(uiSkin, messageDispatcher,
						stockpileComponentUpdater, stockpileGroupDictionary, i18nTranslator, itemTypeDictionary, gameMaterialDictionary, raceDictionary,
						gameContext.getSettlementState().getSettlerRace(), entity.getId(), HaulingAllocation.AllocationPositionType.FURNITURE, currentStockpileComponent.getStockpileSettings());

				}

				outerTable.add(this.stockpileManagementTree).left().pad(4).row();
			}
		}*/

	}
/*
	private void buildSettlerSelectedView(Entity entity) {

		nameTable.clear();
		professionsTable.clear();
		militaryEquipmentTable.clear();
		militaryToggleTable.clear();
		needsTable.clear();
		inventoryTable.clear();
		happinessTable.clear();

		upperRow.clear();
		lowerRow.clear();

		populateSettlerNameTable(entity, nameTable, i18nTranslator, uiSkin, gameContext, messageDispatcher, changeSettlerNameButton, soundAssetDictionary);

		InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
		if (containsSomething(inventoryComponent, null)) {
			inventoryTable.add(inventoryLabel).left().row();

			for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
				inventoryTable.add(new I18nTextWidget(i18nTranslator.getDescription(inventoryEntry.entity), uiSkin, messageDispatcher)).left().row();
			}
		}

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
		EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);

		if (attributes.getConsciousness().equals(Consciousness.DEAD)) {
			upperRow.add(nameTable).top().padRight(5);
			lowerRow.add(inventoryTable).top().padRight(5);
		} else {
			populateProfessionTable(entity);
			populateMilitaryEquipmentTable(militaryComponent, equippedItemComponent);
			populateNeedsTable(needsTable, entity, needLabels);
			populateHappinessTable(entity);
			populateInjuriesTable(entity);
			populateMilitaryToggleTable(entity);

			upperRow.add(nameTable).top().padRight(5);
			if (militaryComponent.isInMilitary()) {
				upperRow.add(militaryEquipmentTable);
			} else {
				upperRow.add(professionsTable);
			}
			upperRow.add(militaryToggleTable).center().padRight(5);

			lowerRow.add(needsTable).top().padRight(5);
			lowerRow.add(inventoryTable).top().padRight(5);
			if (!militaryComponent.isInMilitary()) {
				lowerRow.add(happinessTable).top().padRight(5);
			}
			lowerRow.add(injuriesTable).top().padRight(5);
		}

		entityDescriptionTable.add(upperRow).left().row();
		entityDescriptionTable.add(lowerRow).left();
	}*/

/*	private void addLabel(Entity itemEntity, String defaultI18nKey) {
		I18nText description = itemEntity != null ? i18nTranslator.getDescription(itemEntity) : i18nTranslator.getTranslatedString(defaultI18nKey);
		militaryEquipmentTable.add(new Label(description.toString(), uiSkin)).pad(2).center();
	}*/


/*	private void populateHappinessTable(Entity entity) {
		HappinessComponent happinessComponent = entity.getComponent(HappinessComponent.class);

		Label modifierLabel = buildHappinessModifierLabel(happinessComponent, uiSkin);

		Table headingTable = new Table(uiSkin);
		headingTable.add(new I18nTextWidget(i18nTranslator.getTranslatedString("HAPPINESS_MODIFIER.TITLE"), uiSkin, messageDispatcher));
		headingTable.add(modifierLabel);

		happinessTable.add(headingTable).left().row();

		for (HappinessComponent.HappinessModifier modifier : happinessComponent.currentModifiers()) {
			StringBuilder sb = new StringBuilder();
			sb.append(i18nTranslator.getTranslatedString(modifier.getI18nKey()));
			sb.append(" (");
			int modifierAmount = modifier.modifierAmount;
			if (modifierAmount > 0) {
				sb.append("+");
			}
			sb.append(modifierAmount).append(")");

			happinessTable.add(new Label(sb.toString(), uiSkin)).left().row();
		}

		if (GlobalSettings.DEV_MODE) {
			StatusComponent statusComponent = entity.getComponent(StatusComponent.class);
			if (statusComponent != null && statusComponent.count() > 0) {
				String statuses = "Status: " + statusComponent.getAll().stream().map(s -> s.getClass().getSimpleName()).collect(Collectors.joining(", "));
				happinessTable.add(new Label(statuses, uiSkin)).left().row();
			}
		}
	}*/

/*	private void populateInjuriesTable(Entity entity) {
		injuriesTable.clearChildren();

		StatusComponent statusComponent = entity.getComponent(StatusComponent.class);
		for (StatusEffect statusEffect : statusComponent.getAll()) {
			if (statusEffect.getI18Key() != null) {
				injuriesTable.add(i18nWidgetFactory.createLabel(statusEffect.getI18Key())).left().row();
			}
		}

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		for (I18nText damageDescription : attributes.getBody().getDamageDescriptions(i18nTranslator)) {
			injuriesTable.add(new I18nTextWidget(damageDescription, uiSkin, messageDispatcher)).left().row();
		}
	}*/

	private boolean containsSomething(InventoryComponent inventoryComponent, LiquidContainerComponent liquidContainerComponent) {
		return (inventoryComponent != null && !inventoryComponent.getInventoryEntries().isEmpty()) ||
				(liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0);
	}

/*	private Map<I18nText, Integer> getHaulingTargetDescriptions(Entity itemEntity) {
		Map<I18nText, Integer> haulingTargetDescriptions = new LinkedHashMap<>();

		List<Job> jobsAtLocation = jobStore.getJobsAtLocation(toGridPoint(itemEntity.getLocationComponent().getWorldOrParentPosition()));
		for (Job jobAtLocation : jobsAtLocation) {
			if (jobAtLocation.getType().equals(haulingJobType) && jobAtLocation.getHaulingAllocation() != null &&
					jobAtLocation.getHaulingAllocation().getHauledEntityId() == itemEntity.getId()) {
				I18nText targetDescription = null;
				MapTile targetTile = gameContext.getAreaMap().getTile(jobAtLocation.getHaulingAllocation().getTargetPosition());
				if (targetTile == null) {
					Logger.error("Target tile of hauling allocation is null");
					continue;
				}
				switch (jobAtLocation.getHaulingAllocation().getTargetPositionType()) {
					case ROOM: {
						if (targetTile.getRoomTile() != null) {
							Room room = targetTile.getRoomTile().getRoom();
							targetDescription = new I18nText(room.getRoomName());
						}
						break;
					}
					case CONSTRUCTION: {
						if (targetTile.getConstruction() != null) {
							targetDescription = targetTile.getConstruction().getHeadlineDescription(i18nTranslator);
						}
						break;
					}
					case FURNITURE: {
						Entity targetEntity = entityStore.getById(jobAtLocation.getHaulingAllocation().getTargetId());
						if (targetEntity != null) {
							targetDescription = i18nTranslator.getDescription(targetEntity);
						}
						break;
					}
					case FLOOR: {
						targetDescription = i18nTranslator.getDescription(targetTile);
					}
					case ZONE:
					default: {
						Logger.error("Not yet implemented: getHaulingTargetDescriptions() for hauling target position type " + jobAtLocation.getHaulingAllocation().getTargetPositionType());
					}
				}
				if (targetDescription != null) {
					int quantity = haulingTargetDescriptions.getOrDefault(targetDescription, 0);
					if (jobAtLocation.getHaulingAllocation().getItemAllocation() != null) {
						quantity += jobAtLocation.getHaulingAllocation().getItemAllocation().getAllocationAmount();
					}
					haulingTargetDescriptions.put(targetDescription, quantity);
				} else {
					Logger.error("Target " + jobAtLocation.getHaulingAllocation().getTargetPositionType() + " of hauling allocation is not found");
				}
			}
		}

		return haulingTargetDescriptions;
	}*/

/*	private boolean isItemContainingLiquidOnGroundAndNoneAllocated(Entity entity) {
		if (entity == null) {
			return false;
		}
		LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
		ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
		return entity.getType().equals(ITEM) &&
				(itemAllocationComponent == null || itemAllocationComponent.getNumAllocated() == 0) &&
				liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0 && liquidContainerComponent.getNumAllocated() < 0.001f &&
				((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getItemPlacement().equals(ItemPlacement.ON_GROUND);
	}*/

	private Updatable<Table> textSummary(Entity entity) {
		HappinessComponent happinessComponent = entity.getComponent(HappinessComponent.class);

		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);
		Label happinessLabel = new Label("", managementSkin, "table_value_label");
		table.add(happinessLabel).left().row();

		if (happinessComponent != null) {
			int netHappiness = happinessComponent.getNetModifier();
			String netHappinessString = (netHappiness > 0 ? "+" : "") + netHappiness;
			String happinessText = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.HAPPINESS", Map.of(
					"happinessValue", new I18nWord(netHappinessString)
			)).toString();

			happinessLabel.setText(happinessText);

			happinessLabel.clearListeners();

			Set<HappinessComponent.HappinessModifier> currentModifiers = happinessComponent.currentModifiers();

			List<DecoratedString> tooltipLines = new ArrayList<>();
			for (HappinessComponent.HappinessModifier modifier : currentModifiers) {
				DecoratedString smiley = DecoratedString.drawable(smileyDrawable(modifier.modifierAmount));
				DecoratedString reason = DecoratedString.fromString(happinessReasonText(modifier).toString());
				tooltipLines.add(DecoratedString.of(smiley, reason));
			}

			DecoratedString tooltipString = tooltipLines.get(0);
			for (int i = 1; i < tooltipLines.size(); i++) {
				tooltipString = DecoratedString.of(tooltipString, DecoratedString.linebreak(), tooltipLines.get(i));
			}



			DecoratedStringLabel infoContents = decoratedStringLabelFactory.create(tooltipString, "tooltip-text", mainGameSkin);
			for (Cell<?> cell : infoContents.getCells()) {
				if (cell.getActor() instanceof HorizontalGroup horizontalGroup) {
					horizontalGroup.space(25f);
				}
				cell.padTop(8f).padBottom(8f).padLeft(8f).padRight(8f);
			}
			tooltipFactory.complexTooltip(happinessLabel, infoContents, TooltipFactory.TooltipBackground.LARGE_PATCH_LIGHT);

		}


//		java.util.List<String> behaviourDescriptions = new ArrayList<>();
//		if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
//			java.util.List<I18nText> description = creatureBehaviour.getDescription(i18nTranslator, gameContext, messageDispatcher);
//			for (I18nText i18nText : description) {
//				behaviourDescriptions.add(i18nText.toString());
//			}
//		}


		return updatable;
	}

	private Updatable<Table> happinessIcons(Entity entity) {
		HappinessComponent happinessComponent = entity.getComponent(HappinessComponent.class);

		//TODO: if military, just show injury smiley?
		int MAX_SMILIES = 5;
		Table table = new Table();
		table.defaults().spaceRight(10f);
		Updatable<Table> updatable = Updatable.of(table);
		Runnable updater = () -> {
			table.clear();

			//TODO: add injury
			List<HappinessComponent.HappinessModifier> sorted = new ArrayList<>(happinessComponent.currentModifiers());
			sorted.sort(Comparator.comparing((Function<HappinessComponent.HappinessModifier, Integer>) happinessModifier -> Math.abs(happinessModifier.modifierAmount)).reversed());
			for (int i = 0; i < MAX_SMILIES; i++) {
				if (i < sorted.size()) {
					HappinessComponent.HappinessModifier happinessModifier = sorted.get(i);
					int modifierAmount = happinessModifier.modifierAmount;
					//TODO: not the best code, but should be optimal
					String drawableName = smileyDrawable(modifierAmount);

					I18nText happinessModifierText = happinessReasonText(happinessModifier);
					Image smiley = new Image(mainGameSkin.getDrawable(drawableName));
					tooltipFactory.simpleTooltip(smiley, happinessModifierText, TooltipLocationHint.BELOW);
					table.add(smiley);
				}
			}
		};
		updater.run();
		updatable.regularly(updater);
		return updatable;
	}

	private String smileyDrawable(int modifierAmount) {
		String drawableName;
		if (modifierAmount <= -43) {
			drawableName = MainGameSkin.MISERABLE;
		} else if (modifierAmount <= -31) {
			drawableName = MainGameSkin.SAD;
		} else if (modifierAmount <= -19) {
			drawableName = MainGameSkin.DOWN;
		} else if (modifierAmount <= 0) {
			drawableName = MainGameSkin.NEUTRAL;
		} else if (modifierAmount <= 12) {
			drawableName = MainGameSkin.CHEERY;
		} else if (modifierAmount <= 24) {
			drawableName = MainGameSkin.JOLLY;
		} else if (modifierAmount <= 36) {
			drawableName = MainGameSkin.HAPPY;
		} else {
			drawableName = MainGameSkin.ECSTATIC;
		}
		return drawableName;
	}

	private I18nText happinessReasonText(HappinessComponent.HappinessModifier happinessModifier) {
		int modifierAmount = happinessModifier.modifierAmount;
		StringBuilder modifierBuilder = new StringBuilder();
		modifierBuilder.append(" ");
		if (modifierAmount > 0) {
			modifierBuilder.append("+");
		}
		modifierBuilder.append(modifierAmount);
		I18nText happinessModifierText = i18nTranslator.getTranslatedString(happinessModifier.getI18nKey());
		happinessModifierText.append(new I18nWord(modifierBuilder.toString()));
		return happinessModifierText;
	}


	private Updatable<Table> creatureName(Entity entity) {
		Drawable background = mainGameSkin.getDrawable("asset_bg_ribbon_title");
		Table headerContainer = new Table();
		headerContainer.setBackground(background);
		Updatable<Table> updatable = Updatable.of(headerContainer);



		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		String headerText = attributes.getName().toString();

		Button changeNameButton = new Button(mainGameSkin.getDrawable("icon_edit"));
		changeNameButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
					I18nText dialogTitle = i18nTranslator.getTranslatedString("GUI.DIALOG.RENAME_ROOM_TITLE");
					I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

					final boolean performPause = !gameContext.getGameClock().isPaused();
					if (performPause) {
						messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
					}

					String originalName = attributes.getName().toString();

					TextInputDialog textInputDialog = new TextInputDialog(dialogTitle, originalName, buttonText, menuSkin, (newName) -> {
						if (performPause) {
							messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
						}
						if (!originalName.equals(newName) && !newName.isEmpty()) {
							attributes.getName().rename(newName);
						}
					}, messageDispatcher, EntitySelectedGuiView.this.soundAssetDictionary);
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
			}
		});
		tooltipFactory.simpleTooltip(changeNameButton, "GUI.DIALOG.RENAME_SETTLER_TITLE", TooltipLocationHint.ABOVE);
		changeNameButton.addListener(new ChangeCursorOnHover(changeNameButton, GameCursor.SELECT, messageDispatcher));

		Label headerLabel = new ScaledToFitLabel(headerText, mainGameSkin.get("title-header", Label.LabelStyle.class), background.getMinWidth() - 23f - (2 * (34 + changeNameButton.getWidth())));
		Table editableLabelTable = new Table();
		editableLabelTable.add(headerLabel);
		editableLabelTable.add(changeNameButton).padLeft(23f);
		headerContainer.add(editableLabelTable).center();

		updatable.regularly(() -> {
			headerLabel.setText(attributes.getName().toString());
		});

		return updatable;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ENTITY_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

}
