package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisProgressBar;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.ai.combat.CombatAction;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.*;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.entities.model.physical.creature.status.StatusEffect;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.PopulateSelectItemViewMessage;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.utils.ColorMixer;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.screens.CraftingManagementScreen;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.actions.ButtonAction;
import technology.rocketjump.saul.ui.i18n.I18nString;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ImageButton;
import technology.rocketjump.saul.ui.widgets.*;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.CONTENTS_TO_BE_DUMPED;
import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.MAX_HAPPINESS_VALUE;
import static technology.rocketjump.saul.entities.components.creature.SkillsComponent.MAX_PROFESSIONS;
import static technology.rocketjump.saul.entities.model.EntityType.CREATURE;
import static technology.rocketjump.saul.entities.model.EntityType.ITEM;
import static technology.rocketjump.saul.jobs.SkillDictionary.NULL_PROFESSION;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.ui.Selectable.SelectableType.ENTITY;

@Singleton
public class EntitySelectedGuiView implements GuiView, GameContextAware {

	private final ImageButton UNARMED_IMAGE_BUTTON;
	private final ImageButton UNSHIELDED_IMAGE_BUTTON;
	private final ImageButton UNARMORED_IMAGE_BUTTON;
	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton viewCraftingButton;
	private final IconButton deconstructButton;
	private final IconButton emptyLiquidContainerButton;
	private final EntityStore entityStore;
	private final JobStore jobStore;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final MessageDispatcher messageDispatcher;
	private final JobType haulingJobType;
	private final ImageButton changeSettlerNameButton;
	private final ButtonAction weaponSelectionAction;
	private final ButtonAction shieldSelectionAction;
	private final ButtonAction armorSelectionAction;
	private final ClickableTable squadTextButton;

	private Table outerTable;
	private Table entityDescriptionTable;
	private GameContext gameContext;
	private Label beingDeconstructedLabel;
	private I18nCheckbox militaryToggleCheckbox;
	private Selectable previousSelectable;

	private final Table nameTable;
	private final Table professionsTable;
	private final Table militaryEquipmentTable;
	private final Table needsTable;
	private final Table happinessTable;
	private final Table injuriesTable;
	private final Table inventoryTable;
	private final Table militaryToggleTable;

	private final Table upperRow;
	private final Table lowerRow;

	private final I18nLabel inventoryLabel;

	private final Map<EntityNeed, I18nLabel> needLabels;
	private final ImageButtonFactory imageButtonFactory;
	private final List<ImageButton> cancelButtons = new ArrayList<>();
	private final List<IconOnlyButton> upButtons = new ArrayList<>();
	private final List<IconOnlyButton> downButtons = new ArrayList<>();


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
								 GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory,
								 EntityStore entityStore, JobStore jobStore,
								 I18nWidgetFactory i18nWidgetFactory, JobTypeDictionary jobTypeDictionary,
								 ImageButtonFactory imageButtonFactory, ClickableTableFactory clickableTableFactory,
								 StockpileComponentUpdater stockpileComponentUpdater, StockpileGroupDictionary stockpileGroupDictionary,
								 GameMaterialDictionary gameMaterialDictionary, RaceDictionary raceDictionary,
								 ItemTypeDictionary itemTypeDictionary, TextureAtlasRepository textureAtlasRepository) {
		uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.entityStore = entityStore;
		this.jobStore = jobStore;
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;
		this.imageButtonFactory = imageButtonFactory;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.raceDictionary = raceDictionary;
		this.itemTypeDictionary = itemTypeDictionary;

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
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, CraftingManagementScreen.NAME);
				}
			}
		});

		for (int i = 0; i <= 4; i++) {
			cancelButtons.add(imageButtonFactory.getOrCreate("cancel", true).clone());
			upButtons.add(iconButtonFactory.create("arrow-up").scale(0.5f));
			downButtons.add(iconButtonFactory.create("arrow-down").scale(0.5f));
		}

		deconstructButton = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.SMALL);
		final EntitySelectedGuiView This = this;
		deconstructButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(ENTITY)) {
				Entity entity = selectable.getEntity();
				ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
				if (constructedEntityComponent != null && !constructedEntityComponent.isBeingDeconstructed()) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, selectable.getEntity());
					This.update();
				}
			}
		});

		emptyLiquidContainerButton = iconButtonFactory.create("GUI.EMPTY_CONTAINER_LABEL", "cardboard-box", HexColors.get("#f4ec78"), ButtonStyle.SMALL);
		emptyLiquidContainerButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (isItemContainingLiquidOnGroundAndNoneAllocated(selectable.getEntity())) {
				Entity entity = selectable.getEntity();
				messageDispatcher.dispatchMessage(MessageType.REQUEST_DUMP_LIQUID_CONTENTS, entity);
				This.update();
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
					This.update();
				}
			}
		});

		nameTable = new Table(uiSkin);
		professionsTable = new Table(uiSkin);
		militaryEquipmentTable = new Table(uiSkin);
		needsTable = new Table(uiSkin);
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
		}
	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();

		containerTable.add(outerTable);
	}

	@Override
	public void update() {
		outerTable.clear();
		entityDescriptionTable.clear();

		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if (selectable != null && selectable.type.equals(ENTITY)) {
			boolean sameSelectable = selectable.equals(previousSelectable);
			previousSelectable = selectable;

			Entity entity = selectable.getEntity();
			this.currentStockpileComponent = entity.getComponent(FurnitureStockpileComponent.class);
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
		}

	}

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

		populateSettlerNameTable(entity, nameTable, i18nTranslator, uiSkin, gameContext, messageDispatcher, changeSettlerNameButton);

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
	}

	private void populateMilitaryToggleTable(Entity entity) {
		MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
		militaryToggleCheckbox.setProgrammaticChangeEvents(false);
		if (militaryComponent.isInMilitary()) {
			militaryToggleCheckbox.changeI18nKey("MILITARY", i18nTranslator);
			militaryToggleCheckbox.setChecked(true);
		} else {
			militaryToggleCheckbox.changeI18nKey("CIVILIAN", i18nTranslator);
			militaryToggleCheckbox.setChecked(false);
		}

		squadTextButton.clearChildren();


		militaryToggleTable.add(militaryToggleCheckbox).center().row();
		if (militaryComponent.isInMilitary()) {
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());
			if (squad != null) {
				squadTextButton.add(new Label(squad.getName(), uiSkin));
				squadTextButton.setAction(() -> messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(squad)));
				militaryToggleTable.add(squadTextButton).center().pad(5).row();
			}
		}
	}

	private boolean isTwoHandedWeapon(Entity itemEntity) {
		if (itemEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
			if (attributes.getItemType().getWeaponInfo() != null && attributes.getItemType().getWeaponInfo().isTwoHanded()) {
				return attributes.getItemType().getWeaponInfo().isTwoHanded();
			}
		}
		return false;
	}

	private void populateMilitaryEquipmentTable(MilitaryComponent militaryComponent, EquippedItemComponent equippedItemComponent) {
		// Weapon
		Long assignedWeaponId = militaryComponent.getAssignedWeaponId();
		Entity assignedWeapon = null;
		boolean weaponIsTwoHanded = false;
		if (assignedWeaponId != null) {
			assignedWeapon = gameContext.getEntities().get(assignedWeaponId);
			if (assignedWeapon == null) {
				militaryComponent.setAssignedWeaponId(null);
			} else {
				weaponIsTwoHanded = isTwoHandedWeapon(assignedWeapon);
			}
		}
		ImageButton weaponButton = UNARMED_IMAGE_BUTTON;
		if (assignedWeapon != null) {
			weaponButton = imageButtonFactory.getOrCreate(assignedWeapon);
		}
		weaponButton.setAction(weaponSelectionAction);

		// Shield
		Long assignedShieldId = militaryComponent.getAssignedShieldId();
		Entity assignedShield = null;
		if (assignedShieldId != null) {
			assignedShield = gameContext.getEntities().get(assignedShieldId);
			if (assignedShield == null) {
				militaryComponent.setAssignedShieldId(null);
			}
		}
		ImageButton shieldButton = UNSHIELDED_IMAGE_BUTTON;
		if (assignedShield != null) {
			shieldButton = imageButtonFactory.getOrCreate(assignedShield);
		}
		shieldButton.setAction(shieldSelectionAction);

		// Armour
		Long assignedArmorId = militaryComponent.getAssignedArmorId();
		Entity assignedArmor = null;
		if (assignedArmorId != null) {
			assignedArmor = gameContext.getEntities().get(assignedArmorId);
			if (assignedArmor == null) {
				militaryComponent.setAssignedArmorId(null);
			}
		}
		ImageButton armorButton = UNARMORED_IMAGE_BUTTON;
		if (assignedArmor != null) {
			armorButton = imageButtonFactory.getOrCreate(assignedArmor);
		}
		armorButton.setAction(armorSelectionAction);

		if (equippedItemComponent == null || equippedItemComponent.isMainHandEnabled()) {
			militaryEquipmentTable.add(weaponButton).center().pad(5);
		}

		if (!weaponIsTwoHanded && (equippedItemComponent == null || equippedItemComponent.isOffHandEnabled())) {
			militaryEquipmentTable.add(shieldButton).center().pad(5);
		}
		militaryEquipmentTable.add(armorButton).center().pad(5);
		// Description line
		militaryEquipmentTable.row();
		addLabel(assignedWeapon, "WEAPON.UNARMED");
		if (!weaponIsTwoHanded) {
			addLabel(assignedShield, "WEAPON.NO_SHIELD");
		}
		addLabel(assignedArmor, "WEAPON.NO_ARMOUR");
	}

	private void addLabel(Entity itemEntity, String defaultI18nKey) {
		I18nText description = itemEntity != null ? i18nTranslator.getDescription(itemEntity) : i18nTranslator.getTranslatedString(defaultI18nKey);
		militaryEquipmentTable.add(new Label(description.toString(), uiSkin)).pad(2).center();
	}

	public static void populateSettlerNameTable(Entity entity, Table nameTable, I18nTranslator i18nTranslator, Skin uiSkin,
												GameContext gameContext, MessageDispatcher messageDispatcher, ImageButton renameButton) {
		Cell<I18nTextWidget> nameCell = nameTable.add(new I18nTextWidget(i18nTranslator.getDescription(entity), uiSkin, messageDispatcher)).left();

		if (renameButton != null) {
			renameButton.setAction(() -> {
				// Grabbing translations here so they're always for the correct language
				I18nText renameDialogTitle = i18nTranslator.getTranslatedString("GUI.DIALOG.RENAME_SETTLER_TITLE");
				I18nText descriptionText = i18nTranslator.getTranslatedString("RENAME_DESC");
				I18nText buttonText = i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON");

				final boolean performPause = !gameContext.getGameClock().isPaused();
				if (performPause) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
				}

				CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				String originalName = attributes.getName().toString();

				TextInputDialog textInputDialog = new TextInputDialog(renameDialogTitle, descriptionText, originalName, buttonText, uiSkin, (newName) -> {
					if (performPause) {
						// unpause from forced pause
						messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
					}
					if (!originalName.equals(newName) && !newName.isEmpty()) {
						attributes.getName().rename(newName);
					}
				}, messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, textInputDialog);
			});
			nameTable.add(renameButton).left().padLeft(5).row();
		} else {
			nameCell.row();
		}

		if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			List<I18nText> description;


			CombatStateComponent combatStateComponent = entity.getComponent(CombatStateComponent.class);
			if (combatStateComponent != null && combatStateComponent.isInCombat()) {
				description = creatureBehaviour.getCombatBehaviour().getDescription(i18nTranslator, gameContext, messageDispatcher);
			} else {
				description = creatureBehaviour.getDescription(i18nTranslator, gameContext, messageDispatcher);
			}
			for (I18nText i18nText : description) {
				nameTable.add(new I18nTextWidget(i18nText, uiSkin, messageDispatcher)).left().row();
			}

		} else if (entity.getBehaviourComponent() instanceof CorpseBehaviour) {
			HistoryComponent historyComponent = entity.getComponent(HistoryComponent.class);
			if (historyComponent != null && historyComponent.getDeathReason() != null) {
				DeathReason reason = historyComponent.getDeathReason();

				Map<String, I18nString> replacements = new HashMap<>();
				replacements.put("reason", i18nTranslator.getDictionary().getWord(reason.getI18nKey()));
				I18nText deathDescriptionString = i18nTranslator.getTranslatedWordWithReplacements("NOTIFICATION.DEATH.SHORT_DESCRIPTION", replacements);
				I18nTextWidget label = new I18nTextWidget(deathDescriptionString, uiSkin, messageDispatcher);
				nameTable.add(label).left().row();
			}
		}
	}

	private void populateProfessionTable(Entity entity) {
		SkillsComponent skillsComponent = entity.getComponent(SkillsComponent.class);
		if (skillsComponent == null) {
			return;
		}
		int rowCounter = 0;
		List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();
		List<Table> professionRows = new ArrayList<>();

		for (SkillsComponent.QuantifiedSkill quantifiedSkill : activeProfessions) {
			if (!quantifiedSkill.getSkill().equals(NULL_PROFESSION)) {
				rowCounter++;

				if (skillsComponent.getActiveProfessions().size() > 2) {
					Table orderingTable = new Table(uiSkin);
					final int rowIndex = rowCounter - 1;

					if (rowIndex > 0) {
						IconOnlyButton upButton = upButtons.get(rowIndex);
						upButton.setAction(() -> {
							skillsComponent.swapActiveProfessionPositions(rowIndex - 1, rowIndex);
							messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
							update();
						});
						orderingTable.add(upButton).pad(2).row();
					}

					if (rowIndex < activeProfessions.size() - 2) {
						IconOnlyButton downButton = downButtons.get(rowIndex);
						downButton.setAction(() -> {
							skillsComponent.swapActiveProfessionPositions(rowIndex, rowIndex + 1);
							messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
							update();
						});
						orderingTable.add(downButton).pad(2);
					}

					professionsTable.add(orderingTable).pad(3);
				}

				Table professionRow = new Table(uiSkin);
				professionRow.add(new Label(rowCounter + ". ", uiSkin));
				professionRow.add(new I18nTextWidget(i18nTranslator.getSkilledProfessionDescription(quantifiedSkill.getSkill(),
						quantifiedSkill.getLevel(), ((CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getGender()),
						uiSkin, messageDispatcher));
				professionRow.add(new Label(" (" + quantifiedSkill.getLevel() + ")", uiSkin));
				professionsTable.add(professionRow).align(Align.left).pad(5);
				professionRows.add(professionRow);

				ImageButton cancelButton = cancelButtons.get(rowCounter);
				cancelButton.setAction(() -> {
					skillsComponent.deactivateProfession(quantifiedSkill.getSkill());
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
					update();
				});
				professionsTable.add(cancelButton).pad(5);

				professionsTable.row();

			}
		}

		if (rowCounter < MAX_PROFESSIONS) {
			I18nTextButton addAnotherButton = i18nWidgetFactory.createTextButton("PROFESSION.GUI.ADD_ANOTHER");
			addAnotherButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					gameInteractionStateContainer.setProfessionToReplace(null);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.CHANGE_PROFESSION);
				}
			});
			professionsTable.add(addAnotherButton).colspan(3).pad(5).align(Align.left).row();
		}

	}

	public static void populateNeedsTable(Table needsTable, Entity entity, Map<EntityNeed, I18nLabel> needLabels) {
		NeedsComponent needsComponent = entity.getComponent(NeedsComponent.class);
		if (needsComponent != null) {
			for (Map.Entry<EntityNeed, I18nLabel> entry : needLabels.entrySet()) {
				Double needValue = needsComponent.getValue(entry.getKey());
				if (needValue == null) {
					continue;
				}
				needsTable.add(new I18nLabel(entry.getValue())).pad(5);
				ProgressBar progressBar = new VisProgressBar(0, 100, 1, false);
				progressBar.setValue(Math.round(needValue));
				progressBar.setDisabled(true);
				needsTable.add(progressBar).left().padRight(5);
				needsTable.row();
			}
		}

	}

	private void populateHappinessTable(Entity entity) {
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
	}

	private void populateInjuriesTable(Entity entity) {
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
	}

	public static Label buildHappinessModifierLabel(HappinessComponent happinessComponent, Skin uiSkin) {
		StringBuilder modifierBuilder = new StringBuilder();
		int netModifier = happinessComponent.getNetModifier();
		modifierBuilder.append(" ");
		if (netModifier >= 0) {
			modifierBuilder.append("+");
		}
		modifierBuilder.append(netModifier);
		Label modifierLabel = new Label(modifierBuilder.toString(), uiSkin);
		Label.LabelStyle modifierStyle = new Label.LabelStyle(modifierLabel.getStyle());
		if (netModifier >= 0) {
			modifierStyle.fontColor = ColorMixer.interpolate(0, MAX_HAPPINESS_VALUE, netModifier, Color.YELLOW, Color.GREEN);
		} else {
			modifierStyle.fontColor = ColorMixer.interpolate(0, MAX_HAPPINESS_VALUE, -netModifier, Color.YELLOW, Color.RED);
		}
		modifierLabel.setStyle(modifierStyle);
		return modifierLabel;
	}

	private boolean containsSomething(InventoryComponent inventoryComponent, LiquidContainerComponent liquidContainerComponent) {
		return (inventoryComponent != null && !inventoryComponent.getInventoryEntries().isEmpty()) ||
				(liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0);
	}

	private Map<I18nText, Integer> getHaulingTargetDescriptions(Entity itemEntity) {
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
	}

	private boolean isItemContainingLiquidOnGroundAndNoneAllocated(Entity entity) {
		if (entity == null) {
			return false;
		}
		LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
		ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
		return entity.getType().equals(ITEM) &&
				(itemAllocationComponent == null || itemAllocationComponent.getNumAllocated() == 0) &&
				liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0 && liquidContainerComponent.getNumAllocated() < 0.001f &&
				((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getItemPlacement().equals(ItemPlacement.ON_GROUND);
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
