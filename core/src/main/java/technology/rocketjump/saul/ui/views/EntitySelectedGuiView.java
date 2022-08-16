package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisProgressBar;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.*;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.creature.status.StatusEffect;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ExampleItemDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.utils.ColorMixer;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.screens.CraftingManagementScreen;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
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
	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton viewCraftingButton;
	private final IconButton deconstructButton;
	private final IconButton emptyLiquidContainerButton;
	private final EntityStore entityStore;
	private final ExampleItemDictionary exampleItemDictionary;
	private final JobStore jobStore;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final MessageDispatcher messageDispatcher;
	private final JobType haulingJobType;
	private final ImageButton changeSettlerNameButton;
	private final ImageButton nullProfessionButton1;
	private final ImageButton nullProfessionButton2;

	private Table outerTable;
	private Table entityDescriptionTable;
	private GameContext gameContext;
	private Label beingDeconstructedLabel;

	private final Table nameTable;
	private final Table professionsTable;
	private final Table weaponsTable;
	private final Table needsTable;
	private final Table happinessTable;
	private final Table injuriesTable;
	private final Table inventoryTable;

	private final Table upperRow;
	private final Table lowerRow;

	private final I18nLabel inventoryLabel;

	private final Map<EntityNeed, I18nLabel> needLabels;
	private final ImageButtonFactory imageButtonFactory;
	private final IconButtonFactory iconButtonFactory;
	private final List<ImageButton> cancelButtons = new ArrayList<>();
	private final List<IconOnlyButton> upButtons = new ArrayList<>();
	private final List<IconOnlyButton> downButtons = new ArrayList<>();

	@Inject
	public EntitySelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
								 GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory,
								 EntityStore entityStore, ExampleItemDictionary exampleItemDictionary, JobStore jobStore,
								 I18nWidgetFactory i18nWidgetFactory, JobTypeDictionary jobTypeDictionary, ImageButtonFactory imageButtonFactory, IconButtonFactory iconButtonFactory1) {
		uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.entityStore = entityStore;
		this.exampleItemDictionary = exampleItemDictionary;
		this.jobStore = jobStore;
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;
		this.imageButtonFactory = imageButtonFactory;
		this.iconButtonFactory = iconButtonFactory1;

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
					messageDispatcher.dispatchMessage(MessageType.SHOW_SPECIFIC_CRAFTING, ((CraftingStationBehaviour)behaviourComponent).getCraftingType());
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

		nameTable = new Table(uiSkin);
		professionsTable = new Table(uiSkin);
		weaponsTable = new Table(uiSkin);
		needsTable = new Table(uiSkin);
		happinessTable = new Table(uiSkin);
		injuriesTable = new Table(uiSkin);
		inventoryTable = new Table(uiSkin);

		upperRow = new Table(uiSkin);
		lowerRow = new Table(uiSkin);

		needLabels = i18nWidgetFactory.createNeedsLabels();

		UNARMED_IMAGE_BUTTON = imageButtonFactory.getOrCreate("punch");
		changeSettlerNameButton = imageButtonFactory.getOrCreate("fountain-pen", true).clone();
		nullProfessionButton1 = NULL_PROFESSION.getImageButton().clone();
		nullProfessionButton2 = NULL_PROFESSION.getImageButton().clone();
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
			Entity entity = selectable.getEntity();
			if (entity.isSettler()) {
				buildSettlerSelectedView(entity);
				// TODO description of any dead creatures
			} else {
				entityDescriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(entity), uiSkin, messageDispatcher)).left().row();

				for (EntityComponent component : entity.getAllComponents()) {
					if (component instanceof SelectableDescription) {
						for (I18nText description : ((SelectableDescription) component).getDescription(i18nTranslator, gameContext)) {
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
								entityDescriptionTable.add(new Label("In combat: " + creatureBehaviour.getCombatBehaviour().getCurrentAction().getClass().getSimpleName(), uiSkin)).left().row();
							}
						}
//					SettlerBehaviour behaviourComponent = (SettlerBehaviour) entity.getBehaviourComponent();
//					if (behaviourComponent.getCurrentGoal() != null) {
//						String goal = "Goal: " + behaviourComponent.getCurrentGoal().goal.name;
//						entityDescriptionTable.add(new Label(goal, uiSkin)).left();
//						entityDescriptionTable.row();
//
//						if (behaviourComponent.getCurrentGoal().getCurrentAction() != null) {
//							String action = "Action: " + behaviourComponent.getCurrentGoal().getCurrentAction().getClass().getSimpleName();
//							entityDescriptionTable.add(new Label(action, uiSkin)).left();
//							entityDescriptionTable.row();
//						}
//
//						GoalQueue goalQueue = behaviourComponent.getGoalQueue();
//						entityDescriptionTable.add(new Label("Queued: " + goalQueue.toString(), uiSkin)).left().row();
//					}
					} else if (entity.getType().equals(EntityType.PLANT)) {
//					PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
//
//					PlantSpeciesGrowthStage currentGrowthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());
//					String growthStageDescription = "GS: " + currentGrowthStage.getName() + " progress: " + df.format(attributes.getGrowthStageProgress());
//					entityDescriptionTable.add(new Label(growthStageDescription, uiSkin)).left().row();
//
//					BehaviourComponent behaviourComponent = entity.getBehaviourComponent();
//					if (behaviourComponent instanceof PlantBehaviour) {
//						entityDescriptionTable.add(new Label("Season: " + ((PlantBehaviour) behaviourComponent).getSeasonPlantThinksItIs() +
//								" tTN: "+df.format(((PlantBehaviour) behaviourComponent).getGameSeasonsToNoticeSeasonChange()) +
//								" progress: " + df.format(attributes.getSeasonProgress()), uiSkin)).left().row();
//					}
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
		}
	}

	private void buildSettlerSelectedView(Entity entity) {

		nameTable.clear();
		professionsTable.clear();
		weaponsTable.clear();
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

		if (attributes.getConsciousness().equals(Consciousness.DEAD)) {
			upperRow.add(nameTable).top().padRight(5);
			lowerRow.add(inventoryTable).top().padRight(5);
		} else {
			populateProfessionTable(entity);
			populateWeaponsTable(entity);
			populateNeedsTable(needsTable, entity, needLabels, uiSkin);
			populateHappinessTable(entity);
			populateInjuriesTable(entity);

			upperRow.add(nameTable).top().padRight(5);
			upperRow.add(professionsTable);
			upperRow.add(weaponsTable).padRight(5);

			lowerRow.add(needsTable).top().padRight(5);
			lowerRow.add(inventoryTable).top().padRight(5);
			lowerRow.add(happinessTable).top().padRight(5);
			lowerRow.add(injuriesTable).top().padRight(5);
		}

		entityDescriptionTable.add(upperRow).left().row();
		entityDescriptionTable.add(lowerRow).left();
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

				final GameSpeed currentSpeed = gameContext.getGameClock().getCurrentGameSpeed();
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
				description = creatureBehaviour.getCombatBehaviour().getDescription(i18nTranslator, gameContext);
			} else {
				description = creatureBehaviour.getDescription(i18nTranslator, gameContext);
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
				professionRow.add(new Label(rowCounter +". ", uiSkin));
				professionRow.add(new I18nTextWidget(i18nTranslator.getSkilledProfessionDescription(quantifiedSkill.getSkill(),
						quantifiedSkill.getLevel(), ((CreatureEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getGender()),
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
				public void clicked (InputEvent event, float x, float y) {
					gameInteractionStateContainer.setProfessionToReplace(null);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.CHANGE_PROFESSION);
				}
			});
			professionsTable.add(addAnotherButton).colspan(3).pad(5).align(Align.left).row();
		}

	}

	private void populateWeaponsTable(Entity entity) {
		WeaponSelectionComponent weaponSelectionComponent = entity.getOrCreateComponent(WeaponSelectionComponent.class);

		Optional<ItemType> selectedWeapon = weaponSelectionComponent.getSelectedWeapon();

		ImageButton imageButton;
		if (selectedWeapon.isEmpty()) {
			// Unarmed
			imageButton = UNARMED_IMAGE_BUTTON;
		} else if (hasSelectedWeaponAndAmmoInInventory(entity, selectedWeapon, gameContext)) {
			Optional<Entity> fromInventoryOrEquipped = getSelectedWeaponFromInventoryOrEquipped(entity, selectedWeapon, gameContext);
			imageButton = imageButtonFactory.getOrCreate(fromInventoryOrEquipped.orElse(Entity.NULL_ENTITY));
		} else {
			imageButton = imageButtonFactory.getOrCreateGhostButton(exampleItemDictionary.getExampleItemEntity(selectedWeapon.get(), Optional.empty()));
		}
		weaponsTable.add(imageButton).pad(5);
		imageButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.CHANGE_WEAPON_SELECTION);
		});

		weaponsTable.row();

		if (selectedWeapon.isPresent()) {
			weaponsTable.add(i18nWidgetFactory.createLabel(selectedWeapon.get().getI18nKey())).pad(5);
		} else {
			weaponsTable.add(i18nWidgetFactory.createLabel("WEAPON.UNARMED")).pad(5);
		}
	}

	public static Optional<Entity> getSelectedWeaponFromInventoryOrEquipped(Entity entity, Optional<ItemType> selectedWeapon, GameContext gameContext) {
		if (selectedWeapon.isEmpty()) {
			return Optional.empty();
		} else {
			InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
			InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.findByItemType(selectedWeapon.get(), gameContext.getGameClock());

			if (inventoryEntry != null) {
				// use entitydrawable of actual entity
				return Optional.of(inventoryEntry.entity);
			} else {
				// Might be equipped instead
				EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);
				Entity equippedItem = null;
				if (equippedItemComponent != null) {
					equippedItem = equippedItemComponent.getMainHandItem();
				}
				if (equippedItem != null && equippedItem.getType().equals(ITEM) &&
						((ItemEntityAttributes) equippedItem.getPhysicalEntityComponent().getAttributes()).getItemType().equals(selectedWeapon.get())) {
					return Optional.of(equippedItem);
				} else {
					return Optional.empty();
				}
			}
		}
	}

	public static boolean hasSelectedWeaponAndAmmoInInventory(Entity entity, Optional<ItemType> selectedWeapon, GameContext gameContext) {
		if (selectedWeapon.isEmpty()) {
			return true;
		}

		Optional<Entity> weaponOnEntity = getSelectedWeaponFromInventoryOrEquipped(entity, selectedWeapon, gameContext);
		if (weaponOnEntity.isEmpty()) {
			return false;
		} else {
			if (selectedWeapon.get().getWeaponInfo().getRequiresAmmoType() == null) {
				// No ammo
				return true;
			} else {
				// any ammo in inventory?
				InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
				return inventoryComponent.getInventoryEntries().stream()
						.anyMatch(e -> {
							if (e.entity.getType().equals(ITEM)) {
									ItemEntityAttributes attributes = (ItemEntityAttributes) e.entity.getPhysicalEntityComponent().getAttributes();
								return attributes.getItemType().getIsAmmoType() != null &&
										attributes.getItemType().getIsAmmoType().equals(selectedWeapon.get().getWeaponInfo().getRequiresAmmoType());
							} else {
								return false;
							}
						});
			}
		}
	}

	public static void populateNeedsTable(Table needsTable, Entity entity, Map<EntityNeed, I18nLabel> needLabels, Skin uiSkin) {
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
							targetDescription = i18nTranslator.getDescription(targetTile.getConstruction());
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
					Logger.error("Target "+jobAtLocation.getHaulingAllocation().getTargetPositionType()+" of hauling allocation is not found");
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
				((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getItemPlacement().equals(ItemPlacement.ON_GROUND);
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
