package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.ai.combat.CombatAction;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.components.creature.StatusComponent;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.saul.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
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
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.screens.SettlerManagementScreen;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.Updatable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.I18nString;
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
import java.util.stream.Collectors;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
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
	private final EntityRenderer entityRenderer;
	private final JobType haulingJobType;
//	private final ImageButton changeSettlerNameButton;
//	private final ClickableTable squadTextButton;

	private Table outerTable;
	private List<Updatable<?>> updatables;

//	private Table entityDescriptionTable;
	private GameContext gameContext;
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
	                             EntityRenderer entityRenderer, StockpileComponentUpdater stockpileComponentUpdater, StockpileGroupDictionary stockpileGroupDictionary,
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
		this.entityRenderer = entityRenderer;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.raceDictionary = raceDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;

		haulingJobType = jobTypeDictionary.getByName("HAULING");
/*
		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		entityDescriptionTable = new Table(uiSkin);
		entityDescriptionTable.pad(10);



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

		inventoryLabel = i18nWidgetFactory.createLabel("INVENTORY.CONTAINS.LABEL");

		injuriesTable = new Table(uiSkin);
		inventoryTable = new Table(uiSkin);

		needLabels = i18nWidgetFactory.createNeedsLabels();

		UNARMED_IMAGE_BUTTON = imageButtonFactory.getOrCreate("punch");
		UNSHIELDED_IMAGE_BUTTON = imageButtonFactory.getOrCreate("chicken-oven");
		UNARMORED_IMAGE_BUTTON = imageButtonFactory.getOrCreate("dwarf-face");
		changeSettlerNameButton = imageButtonFactory.getOrCreate("fountain-pen", true).clone();


		squadTextButton = clickableTableFactory.create();
		squadTextButton.setBackground(uiSkin.get(TextButton.TextButtonStyle.class).up);
		squadTextButton.add(new Label("REPLACE ME", uiSkin));


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
		float dropshadowLength = 18f;
		containerTable.add(outerTable).padLeft(dropshadowLength); //Value of drop shadow on bottom for equal distance

		Selectable selectable = gameInteractionStateContainer.getSelectable();
		if (selectable != null && ENTITY == selectable.type) {
			Entity entity = selectable.getEntity();
			if (entity.isSettler()) {
				outerTable.setBackground(mainGameSkin.getDrawable("asset_dwarf_select_bg_wide"));
				boolean isMilitary = SettlerManagementScreen.IS_MILITARY.test(entity);
				Updatable<Actor> settlerName = editableCreatureName(entity);
				Updatable<Table> happinessIcons = happinessIcons(entity);
				Updatable<Table> textSummary = textSummary(entity);
				Table militaryToggle = settlerManagementScreen.militaryToggle(entity, false, s -> populate(containerTable));
				Table weaponSelection = settlerManagementScreen.weaponSelection(entity, 0.8f, s -> populate(containerTable));
				Table professionSelection = settlerManagementScreen.professions(entity, 0.8f, s -> update());
				Updatable<Table> needs = settlerManagementScreen.needs(entity);
				Updatable<Table> inventory = inventory(entity);
				updatables.add(settlerName);
				updatables.add(happinessIcons);
				updatables.add(textSummary);
				updatables.add(needs);
				updatables.add(inventory);

				//Top left first row - name and toggle
				Table topLeftFirstRow = new Table();
				topLeftFirstRow.add(settlerName.getActor()).center();
				topLeftFirstRow.add(militaryToggle).growX().center().spaceLeft(25f);

				//Top left second row - Happiness and status for Civ / Squad for military
				Table topLeftSecondRow = new Table();
				topLeftSecondRow.add(happinessIcons.getActor()).top().left();
				topLeftSecondRow.add(textSummary.getActor()).left().spaceLeft(25f).top().grow();


				//Top Left Column - 2 rows
				Table topLeftColumn = new Table();
				topLeftColumn.add(topLeftFirstRow).left().fillX().spaceBottom(35f).row();
				topLeftColumn.add(topLeftSecondRow).left().top().grow();

				//Top Row - 2 Cols
				Table topRow = new Table();
				topRow.columnDefaults(0).spaceLeft(64f);
				topRow.columnDefaults(1).spaceRight(60f);

				topRow.add(topLeftColumn).left().growX().top();
				if (isMilitary) {
					topRow.add(weaponSelection).right();
				} else {
					topRow.add(professionSelection).right();
				}

				//Bottom Row
				Table bottomRow = new Table();
				bottomRow.add(needs.getActor());
				bottomRow.add(inventory.getActor()).spaceLeft(42).padBottom(11f).bottom().right(); //11f magic number to align with bottom progress bar

				outerTable.add(topRow).fillX().row();
				outerTable.add(bottomRow).left().padBottom(dropshadowLength);

			} else {
				EntityType entityType = entity.getType();
				boolean hasInventory = entity.getComponent(InventoryComponent.class) != null;
				outerTable.top();
				outerTable.setBackground(mainGameSkin.getDrawable("asset_dwarf_select_bg"));

				//TODO : Refactor out common components or modular design
				if (entityType == EntityType.CREATURE) {

					Updatable<Actor> creatureName = creatureName(entity);
					Updatable<Table> happinessIcons = happinessIcons(entity);
					Updatable<Table> textSummary = textSummary(entity);

					updatables.add(creatureName);
					updatables.add(happinessIcons);
					updatables.add(textSummary);

					Table middleRow = new Table();
					middleRow.add(happinessIcons.getActor()).top().right();
					middleRow.add(textSummary.getActor()).left().spaceLeft(25f).grow();

					outerTable.add(creatureName.getActor()).fillX().padTop(67).padBottom(20).row();
					Cell<Table> middleRowCell = outerTable.add(middleRow).expandY().fillX();
					middleRowCell.row();
					if (hasInventory) {
						Updatable<Table> inventory = inventory(entity);
						updatables.add(inventory);
						outerTable.add(inventory.getActor()).padTop(20).padBottom(67 + dropshadowLength);
					} else {
						outerTable.setBackground(mainGameSkin.getDrawable("ENTITY_SELECT_BG_SMALL"));
						middleRowCell.spaceTop(40).top();
					}
				} else {
					Updatable<Actor> name = titleRibbon(i18nTranslator.getDescription(entity).toString());
					Updatable<Actor> descriptions = textDescriptions(entity);


					updatables.add(name);
					updatables.add(descriptions);

					//TODO think aligning from top and lay downwards
					outerTable.add(name.getActor()).top().fillX().padTop(67).padBottom(20).row(); //TODO duplication from above
					outerTable.add(descriptions.getActor()).center().row();

					//TODO: test this
					if (hasInventory) {
						Updatable<Table> inventory = inventory(entity);
						updatables.add(inventory);
						outerTable.add(inventory.getActor()).padTop(20).padBottom(67 + dropshadowLength);
					}
				}
			}
		}
	}

	private Updatable<Actor> textDescriptions(Entity entity) {
		LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
		ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
		EntityAttributes entityAttributes = entity.getPhysicalEntityComponent().getAttributes();
		ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);

		Table table = new Table();
		Updatable<Actor> updatable = Updatable.of(table);

		Runnable updater = () -> {
			Collection<String> descriptions = new ArrayList<>();
			for (EntityComponent component : entity.getAllComponents()) {
				if (component instanceof SelectableDescription selectableDescription) {
					for (I18nText description : selectableDescription.getDescription(i18nTranslator, gameContext, messageDispatcher)) {
						if (!description.isEmpty()) {
							descriptions.add(description.toString());
						}
					}
				}
			}

			if (constructedEntityComponent != null && constructedEntityComponent.isBeingDeconstructed()) {
				descriptions.add(i18nTranslator.translate("GUI.FURNITURE_BEING_REMOVED"));
			}

			if (itemAllocationComponent != null && itemAllocationComponent.getAllocationForPurpose(ItemAllocation.Purpose.CONTENTS_TO_BE_DUMPED) != null) {
				descriptions.add(i18nTranslator.translate("GUI.EMPTY_CONTAINER_LABEL.BEING_ACTIONED"));
			}

			if (entityAttributes instanceof PlantEntityAttributes plant) {
				if (plant.isAfflictedByPests()) {
					descriptions.add(i18nTranslator.translate("CROP.AFFLICTED_BY_PESTS"));
				}
				if (PlantSpeciesType.CROP == plant.getSpecies().getPlantType()) {
					float harvestProgress = 100f * plant.estimatedProgressToHarvesting();
					descriptions.add(i18nTranslator.getHarvestProgress(harvestProgress).toString());
				}
			}
			if (entityAttributes instanceof FurnitureEntityAttributes furniture) {
				if (furniture.getAssignedToEntityId() != null) {
					Entity assignedEntity = entityStore.getById(furniture.getAssignedToEntityId());
					if (assignedEntity == null) {
						Logger.error("Could not find furniture's assignedTo entity by ID " + furniture.getAssignedToEntityId());
					} else {
						descriptions.add(i18nTranslator.getAssignedToLabel(assignedEntity).toString());
					}
				}
				if (furniture.isDestroyed()) {
					descriptions.add(i18nTranslator.getTranslatedString(furniture.getDestructionCause().i18nKey).toString());
				}
			}

			if (entityAttributes instanceof ItemEntityAttributes item) {
				List<String> haulingDescriptions = getHaulingDescriptions(entity);
				descriptions.addAll(haulingDescriptions); //TODO: this might be where we want them clustered/ordered together

				if (item.isDestroyed()) {
					descriptions.add(i18nTranslator.getTranslatedString(item.getDestructionCause().i18nKey).toString());
				}
			}

			if (GlobalSettings.DEV_MODE) {
				if (itemAllocationComponent != null) {
					List<ItemAllocation> itemAllocations = itemAllocationComponent.getAll();
					for (ItemAllocation itemAllocation : itemAllocations) {
						descriptions.add(itemAllocation.toString());
					}
				}

			}

			if (liquidContainerComponent != null && liquidContainerComponent.getLiquidQuantity() > 0) {
				for (I18nText liquidText : liquidContainerComponent.i18nDescription(i18nTranslator)) {
					descriptions.add(liquidText.toString()); //TODO: Test me
				}
			}

			table.clear();
			for (String description : descriptions) {
				//TODO: Decide whether it wraps or just stretches
				Label label = new Label(description, managementSkin, "default-font-18-label");
				table.add(label).grow().row();
			}


		};
		updatable.regularly(updater);
		updatable.update();

		return updatable;
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

			if (entity.getBehaviourComponent() != null && entity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
				outerTable.add(viewCraftingButton);
			}

			ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
			if (constructedEntityComponent != null) {
				if (constructedEntityComponent.canBeDeconstructed()) {
					outerTable.add(deconstructButton);
				}
			}

			if (isItemContainingLiquidOnGroundAndNoneAllocated(entity)) {
				outerTable.add(emptyLiquidContainerButton);
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

	private List<String> getHaulingDescriptions(Entity entity) {
		Map<I18nText, Integer> haulingCounts = getHaulingTargetDescriptions(entity);
		List<String> haulingDescriptions = new ArrayList<>(haulingCounts.size());
		for (Map.Entry<I18nText, Integer> targetDescriptionEntry : haulingCounts.entrySet()) {
			Map<String, I18nString> replacements = new HashMap<>();
			replacements.put("targetDescription", targetDescriptionEntry.getKey());
			replacements.put("quantity", new I18nWord(String.valueOf(targetDescriptionEntry.getValue())));
			I18nText allocationDescription = i18nTranslator.getTranslatedWordWithReplacements("HAULING.ASSIGNMENT.DESCRIPTION", replacements);
			haulingDescriptions.add(allocationDescription.toString());
		}
		return haulingDescriptions;
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
		SkillsComponent skillsComponent = entity.getComponent(SkillsComponent.class);
		FactionComponent factionComponent = entity.getComponent(FactionComponent.class);

		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);
		Label happinessLabel = new Label("", managementSkin, "default-font-18-label");
		Table happinessLabelTooltipContents = new Table();
		tooltipFactory.complexTooltip(happinessLabel, happinessLabelTooltipContents, TooltipFactory.TooltipBackground.LARGE_PATCH_LIGHT);

		Label militaryProfessionLabel = new Label("", managementSkin, "default-font-18-label");

		Label deadLabel = new Label("", managementSkin, "default-font-18-label");
		Label factionLabel = new Label("", managementSkin, "default-font-18-label");

		HorizontalGroup headlineLabels = new HorizontalGroup();
		headlineLabels.addActor(happinessLabel);
		headlineLabels.addActor(militaryProfessionLabel);
		headlineLabels.addActor(deadLabel);
		headlineLabels.addActor(factionLabel);

		table.add(headlineLabels).left().spaceBottom(5f).row();

		Table behaviourTable = new Table();
		table.add(behaviourTable).grow();

		Runnable happinessUpdater = () -> {
			boolean isNotDead = !(entity.getBehaviourComponent() instanceof CorpseBehaviour);
			if (happinessComponent != null && !SettlerManagementScreen.IS_MILITARY.test(entity) && isNotDead) {
				int netHappiness = happinessComponent.getNetModifier();
				String netHappinessString = (netHappiness > 0 ? "+" : "") + netHappiness;
				String happinessText = i18nTranslator.getTranslatedWordWithReplacements("GUI.SETTLER_MANAGEMENT.HAPPINESS", Map.of(
						"happinessValue", new I18nWord(netHappinessString)
				)).toString();

				happinessLabel.setText(happinessText);

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

				happinessLabelTooltipContents.clear();
				happinessLabelTooltipContents.add(infoContents);
			} else {
				happinessLabelTooltipContents.clear();
				happinessLabel.setText("");
			}
		};

		Runnable militaryProfessionUpdater = () -> {
			militaryProfessionLabel.setText("");
			boolean isNotDead = !(entity.getBehaviourComponent() instanceof CorpseBehaviour);
			if (skillsComponent != null && SettlerManagementScreen.IS_MILITARY.test(entity) && isNotDead) {
				String assignedWeaponText = settlerManagementScreen.getAssignedWeaponText(entity, skillsComponent);
				militaryProfessionLabel.setText(assignedWeaponText);
			}
		};

		Runnable deadLabelUpdater = () -> {
			deadLabel.setText("");
			if (entity.getBehaviourComponent() instanceof CorpseBehaviour corpseBehaviour) {
				String deadText = corpseBehaviour.getDescription(i18nTranslator, gameContext, messageDispatcher)
						.stream()
						.map(I18nText::toString)
						.collect(Collectors.joining("\n"));
				deadLabel.setText(deadText);
			}
		};

		Runnable factionLabelUpdater = () -> {
			factionLabel.setText("");
			boolean isNotDead = !(entity.getBehaviourComponent() instanceof CorpseBehaviour);
			if (isNotDead && factionComponent != null && (factionComponent.getFaction() == Faction.WILD_ANIMALS || factionComponent.getFaction() == Faction.MERCHANTS || factionComponent.getFaction() == Faction.MONSTERS)) {
				factionLabel.setText(i18nTranslator.translate(factionComponent.getFaction().i18nKey));
			}
		};

		Runnable descriptionUpdater = () -> {
			behaviourTable.clear();

			java.util.List<String> behaviourDescriptions = new ArrayList<>();
			if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
				java.util.List<I18nText> description = creatureBehaviour.getDescription(i18nTranslator, gameContext, messageDispatcher);
				for (I18nText i18nText : description) {
					behaviourDescriptions.add(i18nText.toString());
				}

				if (GlobalSettings.DEV_MODE) {
					CombatStateComponent combatStateComponent = entity.getComponent(CombatStateComponent.class);
					if (combatStateComponent != null && combatStateComponent.isInCombat()) {
						CombatAction currentAction = creatureBehaviour.getCombatBehaviour().getCurrentAction();
						if (currentAction == null) {
							behaviourDescriptions.add("In combat");
						} else {
							behaviourDescriptions.add("In combat: " + currentAction.getClass().getSimpleName());
						}
					}
				}
			}

			for (String behaviourDescription : behaviourDescriptions) {
				Label label = new Label(behaviourDescription, managementSkin, "default-font-16-label") {
					@Override
					public float getWidth() {
						return getParent().getWidth();
					}
				};
				label.setWrap(true);

				behaviourTable.add(label).growX().row();
			}
		};


		updatable.regularly(happinessUpdater);
		updatable.regularly(militaryProfessionUpdater);
		updatable.regularly(descriptionUpdater);
		updatable.regularly(deadLabelUpdater);
		updatable.regularly(factionLabelUpdater);
		updatable.update();
		return updatable;
	}


	private Updatable<Table> happinessIcons(Entity entity) {
		HappinessComponent happinessComponent = entity.getComponent(HappinessComponent.class);
		StatusComponent statusComponent = entity.getComponent(StatusComponent.class);
		FactionComponent factionComponent = entity.getComponent(FactionComponent.class);
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		Table table = new Table();
		table.defaults().spaceRight(8f);
		Updatable<Table> updatable = Updatable.of(table);
		Runnable updater = () -> {
			table.clear();
			boolean isMilitary = SettlerManagementScreen.IS_MILITARY.test(entity);
			boolean isSettlement = factionComponent != null && factionComponent.getFaction() == Faction.SETTLEMENT;
			boolean unknownHappiness = isMilitary || !isSettlement || entity.getBehaviourComponent() instanceof CorpseBehaviour; //Dead don't display happiness

			List<String> ailments = new ArrayList<>();
			for (StatusEffect statusEffect : statusComponent.getAll()) {
				if (statusEffect.getI18Key() != null) {
					ailments.add(i18nTranslator.translate(statusEffect.getI18Key()));
				}
			}
			for (I18nText damageDescription : attributes.getBody().getDamageDescriptions(i18nTranslator)) {
				ailments.add(damageDescription.toString());
			}

			final int MAX_SMILIES;
			if (unknownHappiness) {
				MAX_SMILIES = 0;
			} else if (ailments.isEmpty()) {
				MAX_SMILIES = 5;
			} else {
				MAX_SMILIES = 4;
			}

			if (!ailments.isEmpty()) {
				DecoratedString tooltipString = DecoratedString.fromString(ailments.get(0));
				for (int i = 1; i < ailments.size(); i++) {
					tooltipString = DecoratedString.of(tooltipString, DecoratedString.linebreak(), DecoratedString.fromString(ailments.get(i)));
				}
				DecoratedStringLabel tooltipContents = decoratedStringLabelFactory.create(tooltipString, "tooltip-text", mainGameSkin);

				Image injurySmiley = new Image(mainGameSkin.getInjuredSmiley(factionComponent));
				tooltipFactory.complexTooltip(injurySmiley, tooltipContents, TooltipFactory.TooltipBackground.LARGE_PATCH_LIGHT);
				table.add(injurySmiley);
			} else if (unknownHappiness) {
				Image notInjuredSmiley = new Image(mainGameSkin.getNotInjuredSmiley(factionComponent));
				tooltipFactory.simpleTooltip(notInjuredSmiley, "BODY_STRUCTURE.DAMAGE.NOT_INJURED", TooltipLocationHint.BELOW);
				table.add(notInjuredSmiley);
			}

			if (happinessComponent != null && isSettlement) {
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

	private Updatable<Actor> creatureName(Entity entity) {
		final String headerText;
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getName() != null) {
			headerText = attributes.getName().toString();
		} else {
			headerText = i18nTranslator.getCreatureDescription(entity, attributes).toString();
		}

		return titleRibbon(headerText);
	}

	private Updatable<Actor> titleRibbon(String headerText) {
		Label headerLabel = new Label(headerText, mainGameSkin.get("title-header", Label.LabelStyle.class));
		Container<Label> headerContainer = new Container<>(headerLabel);
		headerContainer.setBackground(mainGameSkin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));
		return Updatable.of(headerContainer);
	}

	private Updatable<Actor> editableCreatureName(Entity entity) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		String headerText = attributes.getName().toString();

		Drawable changeButtonDrawable = mainGameSkin.getDrawable("icon_edit");
		Button changeNameButton = new Button(changeButtonDrawable);
		changeNameButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
					I18nText dialogTitle = i18nTranslator.getTranslatedString("GUI.DIALOG.RENAME_SETTLER_TITLE");
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

		Drawable background = mainGameSkin.getDrawable("Dwarf_Name_Banner");
		float pencilWidth = changeButtonDrawable.getMinWidth();
		float indentWidth = 27f;
		float labelMaxWidth = background.getMinWidth() - indentWidth - pencilWidth - pencilWidth - indentWidth;

		Label headerLabel = new ScaledToFitLabel(headerText, mainGameSkin.get("title-header", Label.LabelStyle.class), labelMaxWidth);
		headerLabel.setAlignment(Align.center);

		Table buttonTable = new Table();
		buttonTable.setBackground(background);
		buttonTable.add(new Actor()).width(pencilWidth).padLeft(indentWidth);
		buttonTable.add(headerLabel).width(labelMaxWidth);
		buttonTable.add(changeNameButton).padRight(indentWidth);

		Updatable<Actor> updatable = Updatable.of(buttonTable);
		updatable.regularly(() -> {
			headerLabel.setText(attributes.getName().toString());
		});

		return updatable;
	}

	private Updatable<Table> inventory(Entity entity) {
		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);
		InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
		DecorationInventoryComponent decorationInventoryComponent = entity.getComponent(DecorationInventoryComponent.class);

		int maxSlots = 8;
		updatable.regularly(() -> {
			table.clear();

			List<Entity> inventoryEntities = new ArrayList<>();
			if (inventoryComponent != null) {
				inventoryEntities.addAll(inventoryComponent.getInventoryEntries().stream().map(entry -> entry.entity).toList());
			}
			if (decorationInventoryComponent != null) {
				inventoryEntities.addAll(decorationInventoryComponent.getDecorationEntities());
			}
			Iterator<Entity> inventoryIterator = inventoryEntities.iterator();



			for (int slotIndex = 0; slotIndex < maxSlots; slotIndex++) {
				Stack entityStack = new Stack();


				Drawable emptyBackgroundDrawable = mainGameSkin.getDrawable("asset_dwarf_select_inventory_bg");

				Container<Image> backgroundContainer = new Container<>(new Image(emptyBackgroundDrawable));
				backgroundContainer.bottom().right();
				entityStack.add(backgroundContainer);

				if (inventoryIterator.hasNext()) {
					Entity inventoryItem = inventoryIterator.next();

					ItemEntityAttributes attributes = (ItemEntityAttributes) inventoryItem.getPhysicalEntityComponent().getAttributes();
					int quantity = attributes.getQuantity();



					EntityDrawable entityDrawable = new EntityDrawable(inventoryItem, entityRenderer, true, messageDispatcher);
					entityDrawable.setMinSize(emptyBackgroundDrawable.getMinWidth(), emptyBackgroundDrawable.getMinHeight());

					Image entityImage = new Image(entityDrawable);
					tooltipFactory.simpleTooltip(entityImage, i18nTranslator.getDescription(inventoryItem), TooltipLocationHint.BELOW); //TODO: need to do something to prevent sticky tooltips

					Container<Image> entityImageContainer = new Container<>(entityImage);
					entityImageContainer.bottom().right();

					entityStack.add(entityImageContainer);

					if (quantity > 1) {
						Label amountLabel = new Label(String.valueOf(quantity), managementSkin, "entity_drawable_quantity_label");
						amountLabel.setAlignment(Align.center);
						amountLabel.layout();

						float xOffset = 10f;
						float yOffset = 10f;
						float extraWidth = backgroundContainer.getPrefWidth() + xOffset - amountLabel.getPrefWidth();
						float extraHeight = backgroundContainer.getPrefHeight() + yOffset - amountLabel.getPrefHeight();


						Table amountTable = new Table();
						amountTable.add(amountLabel).left().top();
						amountTable.add(new Container<>()).expandX().width(extraWidth).row();
						amountTable.add(new Container<>()).colspan(2).height(extraHeight).expandY();
						entityStack.add(amountTable);
					}
					table.add(entityStack).bottom().spaceLeft(8f);
				} else {
					table.add(entityStack).bottom().spaceLeft(18f);
				}
			}
		});

		updatable.update();
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
