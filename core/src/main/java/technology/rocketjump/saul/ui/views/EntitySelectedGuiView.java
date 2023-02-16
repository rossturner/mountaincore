package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.ai.combat.CombatAction;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.*;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.*;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.saul.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
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
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.utils.ColorMixer;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.screens.ManagementScreenName;
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
import technology.rocketjump.saul.ui.widgets.crafting.CraftingHintWidgetFactory;
import technology.rocketjump.saul.ui.widgets.furniture.ProductionExportFurnitureWidget;
import technology.rocketjump.saul.ui.widgets.furniture.ProductionImportFurnitureWidget;
import technology.rocketjump.saul.ui.widgets.rooms.PriorityWidget;
import technology.rocketjump.saul.ui.widgets.text.DecoratedString;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabel;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabelFactory;

import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.entities.model.EntityType.ITEM;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.ui.Selectable.SelectableType.ENTITY;

@Singleton
public class EntitySelectedGuiView implements GuiView, GameContextAware {

	private static final int MAX_DWARF_NAME_PLUS_15PC = 31;
	protected final GameInteractionStateContainer gameInteractionStateContainer;
	private final SoundAssetDictionary soundAssetDictionary;
	private final I18nTranslator i18nTranslator;
	private final EntityStore entityStore;
	private final JobStore jobStore;
	private final TooltipFactory tooltipFactory;
	private final ProductionImportFurnitureWidget productionImportFurnitureWidget;
	private final ProductionExportFurnitureWidget productionExportFurnitureWidget;
	private final MessageDispatcher messageDispatcher;
	private final MainGameSkin mainGameSkin;
	private final ManagementSkin managementSkin;
	private final MenuSkin menuSkin;
	private final DecoratedStringLabelFactory decoratedStringLabelFactory;
	private final EntityRenderer entityRenderer;
	private final JobType haulingJobType;
	private final ButtonFactory buttonFactory;
	private final CraftingHintWidgetFactory craftingHintWidgetFactory;

	private Table outerTable;
	private List<Updatable<?>> updatables;

	private GameContext gameContext;
	//TODO: remove me asap
	private final SettlerManagementScreen settlerManagementScreen;


	// For stockpiles
	private boolean showStockpileSettings = false;
	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final RaceDictionary raceDictionary;
	private final ItemTypeDictionary itemTypeDictionary;

	@Inject
	public EntitySelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
	                             GameInteractionStateContainer gameInteractionStateContainer,
	                             EntityStore entityStore, JobStore jobStore,
	                             JobTypeDictionary jobTypeDictionary,
	                             TooltipFactory tooltipFactory,
	                             ProductionImportFurnitureWidget productionImportFurnitureWidget, ProductionExportFurnitureWidget productionExportFurnitureWidget,
	                             DecoratedStringLabelFactory decoratedStringLabelFactory,
	                             EntityRenderer entityRenderer, ButtonFactory buttonFactory, CraftingHintWidgetFactory craftingHintWidgetFactory,
	                             StockpileComponentUpdater stockpileComponentUpdater,
	                             StockpileGroupDictionary stockpileGroupDictionary,
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
		this.productionImportFurnitureWidget = productionImportFurnitureWidget;
		this.productionExportFurnitureWidget = productionExportFurnitureWidget;
		this.decoratedStringLabelFactory = decoratedStringLabelFactory;
		this.entityRenderer = entityRenderer;
		this.buttonFactory = buttonFactory;
		this.craftingHintWidgetFactory = craftingHintWidgetFactory;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.raceDictionary = raceDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;

		haulingJobType = jobTypeDictionary.getByName("HAULING");
		this.settlerManagementScreen = settlerManagementScreen;
	}

	protected Entity getSelectedEntity() {
		Selectable selectable = gameInteractionStateContainer.getSelectable();
		if (selectable != null && ENTITY == selectable.type) {
			return selectable.getEntity();
		} else {
			return null;
		}
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();
		showStockpileSettings = false;
		updatables = new ArrayList<>();
		outerTable = new Table();
		outerTable.setTouchable(Touchable.enabled);
		float dropshadowLength = 18f;
		containerTable.add(outerTable).padLeft(dropshadowLength); //Value of drop shadow on bottom for equal distance


		Entity entity = getSelectedEntity();
		if (entity != null) {
			if (entity.isSettler()) {
				outerTable.setBackground(mainGameSkin.getDrawable("asset_dwarf_select_bg_wide"));
				boolean isMilitary = SettlerManagementScreen.IS_MILITARY.test(entity);
				Updatable<Actor> settlerName = editableCreatureName(entity);
				Updatable<Table> happinessIcons = happinessIcons(entity);
				Updatable<Table> textSummary = textSummary(entity);
				Updatable<Table> debugTextSummary = debugTextSummary(entity);
				Table militaryToggle = settlerManagementScreen.militaryToggle(entity, false, s -> populate(containerTable));
				Table weaponSelection = settlerManagementScreen.weaponSelection(entity, 0.8f, s -> populate(containerTable));
				Table professionSelection = settlerManagementScreen.professions(entity, 0.8f, s -> update());
				Updatable<Table> needs = settlerManagementScreen.needs(entity);
				Updatable<Table> inventory = inventory(entity);
				Table squadEmblem = squadEmblem(entity);
				updatables.add(settlerName);
				updatables.add(happinessIcons);
				updatables.add(textSummary);
				updatables.add(needs);
				updatables.add(inventory);
				updatables.add(debugTextSummary);

				//Top left first row - name and toggle
//				Table topLeftFirstRow = new Table();
//				topLeftFirstRow.add(settlerName.getActor()).left();
//				topLeftFirstRow.add(militaryToggle).growX().center().spaceLeft(25f);

				//Top left second row - Happiness and status for Civ / Squad for military
				Table topLeftSecondRow = new Table();
				topLeftSecondRow.add(happinessIcons.getActor()).top().left();
				topLeftSecondRow.add(textSummary.getActor()).left().spaceLeft(25f).top().grow();


				//Top Left Column - 2 rows
				Table topLeftColumn = new Table();
				topLeftColumn.add(settlerName.getActor()).left().growX().spaceBottom(35f);
				topLeftColumn.add(militaryToggle).center().growX().spaceBottom(35f);
				topLeftColumn.row();
				topLeftColumn.add(topLeftSecondRow).left().top().grow();
				topLeftColumn.add(squadEmblem).center();
				topLeftColumn.row();
				if (GlobalSettings.DEV_MODE) {
					topLeftColumn.add(debugTextSummary.getActor()).left().top().grow().colspan(2).row();
				}

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
					if (entity.getComponent(InventoryComponent.class) != null) {
						Updatable<Table> inventory = inventory(entity);
						updatables.add(inventory);
						outerTable.add(inventory.getActor()).padTop(20).padBottom(67 + dropshadowLength);
					} else {
						outerTable.setBackground(mainGameSkin.getDrawable("ENTITY_SELECT_BG_SMALL"));
						middleRowCell.spaceTop(40).top();
					}
				} else {
					Updatable<Actor> name = titleRibbon(i18nTranslator.getDescription(entity).toString());
					Updatable<Table> progressBars = progressBars(entity);
					Updatable<Table> descriptions = textDescriptions(entity);
					Updatable<Table> actionButtons = actionButtons(entity);
					Updatable<Table> inventory = inventory(entity);

					updatables.add(name);
					updatables.add(actionButtons);
					updatables.add(descriptions);
					updatables.add(progressBars);
					updatables.add(inventory);


					actionButtons.getActor().layout();

					//TODO duplication from above
					outerTable.add(new Container<>()).left().width(actionButtons.getActor().getPrefWidth()).padTop(67).padLeft(67);
					outerTable.add(name.getActor()).fillX().padTop(67);
					outerTable.add(actionButtons.getActor()).right().padTop(67).padRight(67).padLeft(18);
					outerTable.row();

					Table viewContents = new Table();
					viewContents.defaults().growY().spaceBottom(20);
					Updatable<Table> viewContentsUpdatable = Updatable.of(viewContents); //Absolutely despise what i've done here, essentially empty tables still pad things out
					outerTable.add(viewContents).colspan(3).growY().padRight(67).padLeft(67).padTop(20).row();

					if (entity.getBehaviourComponent() instanceof ProductionImportFurnitureBehaviour) {
						productionImportFurnitureWidget.setFurnitureEntity(entity);
					} else if (entity.getBehaviourComponent() instanceof ProductionExportFurnitureBehaviour) {
						productionExportFurnitureWidget.setFurnitureEntity(entity);
					}

					viewContentsUpdatable.regularly(() -> {
						viewContents.clear();

						List<Prioritisable> prioritisables = entity.getAllComponents().stream()
								.filter(Prioritisable.class::isInstance)
								.map(Prioritisable.class::cast)
								.toList();
						if (prioritisables.size() > 1) {
							Logger.warn("Multiple prioritisables have been found on entity {}",
									prioritisables.stream()
											.map(Prioritisable::getClass)
											.map(Class::getSimpleName)
											.collect(Collectors.joining()));
						} else if (prioritisables.size() == 1) {
							Prioritisable prioritisable = prioritisables.get(0);
							viewContents.add(new PriorityWidget(prioritisable, mainGameSkin, tooltipFactory, messageDispatcher)).center().row();
						}

						if (progressBars.getActor().hasChildren()) {
							viewContents.add(progressBars.getActor()).center().row();
						}

						if (descriptions.getActor().hasChildren()) {
							viewContents.add(descriptions.getActor()).center().row();
						}

						if (entity.getBehaviourComponent() instanceof TradingImportFurnitureBehaviour) {
							viewContents.add(productionExportFurnitureWidget).center().row();
						} else if (entity.getBehaviourComponent() instanceof TradingExportFurnitureBehaviour) {
							viewContents.add(productionImportFurnitureWidget).center().row();
						} else if (entity.getBehaviourComponent() instanceof ProductionImportFurnitureBehaviour) {
							viewContents.add(productionImportFurnitureWidget).center().row();
						} else if (entity.getBehaviourComponent() instanceof ProductionExportFurnitureBehaviour) {
							viewContents.add(productionExportFurnitureWidget).center().row();
						}

						//TODO: test this
						if (entity.getComponent(InventoryComponent.class) != null) {
							viewContents.add(inventory.getActor()).row();
						} else {
							outerTable.setBackground(mainGameSkin.getDrawable("ENTITY_SELECT_BG_SMALL"));
						}


					});
					viewContentsUpdatable.update();

					updatables.add(viewContentsUpdatable);
					if (entity.getComponent(FurnitureStockpileComponent.class) != null) {
						StockpileManagementTree stockpileManagementTree = new StockpileManagementTree(mainGameSkin, messageDispatcher,
								stockpileComponentUpdater, stockpileGroupDictionary, i18nTranslator, itemTypeDictionary, gameMaterialDictionary, raceDictionary,
								gameContext.getSettlementState().getSettlerRace(), entity.getId(), HaulingAllocation.AllocationPositionType.FURNITURE, entity.getComponent(FurnitureStockpileComponent.class).getStockpileSettings());
						stockpileManagementTree.setSize(1700, 600);

						CollapsibleWidget stockpileTreeCollapsible = new CollapsibleWidget(stockpileManagementTree, true);
						Updatable<CollapsibleWidget> stockpileTreeUpdatable = Updatable.of(stockpileTreeCollapsible);

						stockpileTreeUpdatable.regularly(() -> {
							//WTF was i doing with this boolean code
							if (showStockpileSettings && stockpileTreeCollapsible.isCollapsed()) {
								stockpileTreeCollapsible.setCollapsed(false, false);
							} else if (!showStockpileSettings && !stockpileTreeCollapsible.isCollapsed()) {
								stockpileTreeCollapsible.setCollapsed(true, false);
							}
						});
						updatables.add(stockpileTreeUpdatable);

						outerTable.add(stockpileTreeCollapsible).left().colspan(3).padLeft(67).padTop(20).padRight(67).row();
					}

					outerTable.add(new Container<>()).colspan(3).padBottom(67 + dropshadowLength);
				}
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
	}


	private Updatable<Table> actionButtons(Entity entity) {
		ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);
		FurnitureStockpileComponent furnitureStockpileComponent = entity.getComponent(FurnitureStockpileComponent.class);

		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);

		table.defaults().space(18);

		Runnable updater = () -> {
			table.clear();

			if (furnitureStockpileComponent != null) {
				Container<Button> stockpileSettingsContainer = new Container<>();
				if (showStockpileSettings) {
					stockpileSettingsContainer.setBackground(mainGameSkin.getDrawable("asset_selection_bg_cropped"));
				}

				Button stockpileSettingsButton = new Button(mainGameSkin.getDrawable("btn_settings"));
				stockpileSettingsButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						showStockpileSettings = !showStockpileSettings;
						update();
					}
				});
				buttonFactory.attachClickCursor(stockpileSettingsButton, GameCursor.SELECT);
				tooltipFactory.simpleTooltip(stockpileSettingsButton, "GUI.DIALOG.STOCKPILE_MANAGEMENT.TITLE", TooltipLocationHint.ABOVE);
				stockpileSettingsContainer.setActor(stockpileSettingsButton);
				table.add(stockpileSettingsContainer);
			}

			if (constructedEntityComponent != null && constructedEntityComponent.canBeDeconstructed()) {
				Container<Button> deconstructContainer = new Container<>();
				if (constructedEntityComponent.isBeingDeconstructed()) {
					deconstructContainer.setBackground(mainGameSkin.getDrawable("asset_selection_bg_cropped"));
				}

				Button deconstructButton = new Button(mainGameSkin.getDrawable("btn_demolish_small"));
				deconstructButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						super.clicked(event, x, y);
						if (!constructedEntityComponent.isBeingDeconstructed()) {
							messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, entity);
							update();
						}
					}
				});

				if (constructedEntityComponent.isBeingDeconstructed()) {
					buttonFactory.disable(deconstructButton);
				}
				buttonFactory.attachClickCursor(deconstructButton, GameCursor.SELECT);
				tooltipFactory.simpleTooltip(deconstructButton, "GUI.DECONSTRUCT_LABEL", TooltipLocationHint.ABOVE);
				deconstructContainer.setActor(deconstructButton);
				table.add(deconstructContainer);
			}

			if (entity.getBehaviourComponent() instanceof CraftingStationBehaviour) {
				Container<Button> craftingButtonContainer = new Container<>();
				Button craftingButton = new Button(managementSkin.getDrawable("btn_recipe"));
				craftingButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						super.clicked(event, x, y);
						messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, ManagementScreenName.CRAFTING.name());
					}
				});

				buttonFactory.attachClickCursor(craftingButton, GameCursor.SELECT);
				tooltipFactory.simpleTooltip(craftingButton, "GUI.CRAFTING_MANAGEMENT.TITLE", TooltipLocationHint.ABOVE);
				craftingButtonContainer.setActor(craftingButton);
				table.add(craftingButtonContainer);
			}

			if (isItemContainingLiquidOnGroundAndNoneAllocated(entity)) {
				TextButton emptyContainerButton = new TextButton(i18nTranslator.translate("GUI.EMPTY_CONTAINER_LABEL"), mainGameSkin, "btn_entity_selected_action");
				emptyContainerButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						super.clicked(event, x, y);
						messageDispatcher.dispatchMessage(MessageType.REQUEST_DUMP_LIQUID_CONTENTS, entity);
						update();

					}
				});
				buttonFactory.attachClickCursor(emptyContainerButton, GameCursor.SELECT);
				table.add(emptyContainerButton);
			}
		};
		updatable.regularly(updater);
		updatable.update();

		return updatable;
	}


	private Updatable<Table> progressBars(Entity entity) {
		LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);

		updatable.regularly(() -> {
			table.clear();

			if (liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null && liquidContainerComponent.getLiquidQuantity() > 0) {

				Label label = new Label("", managementSkin, "default-font-18-label");
				label.setAlignment(Align.right);

				float liquidQuantity = liquidContainerComponent.getLiquidQuantity();
				float min = 0;
				float max = liquidQuantity;
				float midpoint = max / 2f;
				float value = liquidContainerComponent.getNumUnallocated();

				ProgressBar progressBar = new ProgressBar(min, max, LiquidContainerComponent.SMALL_AMOUNT, false, managementSkin);
				progressBar.setValue(value);
				progressBar.setDisabled(true);
				progressBar.setHeight(42);
				Color progressBarColour;
				if (value >= midpoint) {
					progressBarColour = ColorMixer.interpolate(midpoint, liquidQuantity, value, managementSkin.getColor("progress_bar_yellow"), managementSkin.getColor("progress_bar_green"));
				} else {
					progressBarColour = ColorMixer.interpolate(min, midpoint, value, managementSkin.getColor("progress_bar_red"), managementSkin.getColor("progress_bar_yellow"));
				}
				ProgressBar.ProgressBarStyle clonedStyle = new ProgressBar.ProgressBarStyle(progressBar.getStyle());
				if (clonedStyle.knobBefore instanceof NinePatchDrawable ninePatchDrawable) {
					clonedStyle.knobBefore = ninePatchDrawable.tint(progressBarColour);
				}
				progressBar.setStyle(clonedStyle);


				I18nText liquidDescription = i18nTranslator.getLiquidDescription(liquidContainerComponent.getTargetLiquidMaterial(), max);
				label.setText(i18nTranslator.getTranslatedWordWithReplacements("GUI.AVAILABLE_PROGRESS_BAR", Map.of("item", liquidDescription)).toString());

				table.add(label).spaceRight(28);
				table.add(progressBar).width(318).growX().row();

			}


			if (entity.getPhysicalEntityComponent().getAttributes() instanceof PlantEntityAttributes plant) {
				if (PlantSpeciesType.CROP == plant.getSpecies().getPlantType()) {
					float harvestProgress = 100f * plant.estimatedProgressToHarvesting();
					Label label = new Label(i18nTranslator.getHarvestProgress(harvestProgress).toString(), managementSkin, "default-font-18-label");
					label.setAlignment(Align.right);

					ProgressBar progressBar = new ProgressBar(0, 100, 1, false, managementSkin);
					progressBar.setValue(harvestProgress);
					progressBar.setDisabled(true);
					progressBar.setHeight(42);
					Color progressBarColour = managementSkin.getColor("progress_bar_green");

					ProgressBar.ProgressBarStyle clonedStyle = new ProgressBar.ProgressBarStyle(progressBar.getStyle());
					if (clonedStyle.knobBefore instanceof NinePatchDrawable ninePatchDrawable) {
						clonedStyle.knobBefore = ninePatchDrawable.tint(progressBarColour);
					}
					progressBar.setStyle(clonedStyle);

					table.add(label).spaceRight(28);
					table.add(progressBar).width(318).growX().row();

				}
			}
		});

		updatable.update();
		return updatable;
	}


	private Updatable<Table> textDescriptions(Entity entity) {
		ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
		EntityAttributes entityAttributes = entity.getPhysicalEntityComponent().getAttributes();
		ConstructedEntityComponent constructedEntityComponent = entity.getComponent(ConstructedEntityComponent.class);

		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);

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

				descriptions.addAll(craftingHintWidgetFactory.getCraftingRecipeDescriptions(item.getItemType(), item.getPrimaryMaterial()));
			}

			table.clear();
			for (String description : descriptions) {
				//TODO: Decide whether it wraps or just stretches
				Label label = new Label(description, managementSkin, "default-font-18-label");
				table.add(label).grow().row();
			}
			if (GlobalSettings.DEV_MODE) {
				if (itemAllocationComponent != null) {
					List<ItemAllocation> itemAllocations = itemAllocationComponent.getAll();
					Label.LabelStyle debugStyle = new Label.LabelStyle(managementSkin.get("default-font-18-label", Label.LabelStyle.class));
					debugStyle.fontColor = Color.PURPLE;
					for (ItemAllocation itemAllocation : itemAllocations) {
						Label label = new Label(itemAllocation.toString(), debugStyle);
						table.add(label).grow().row();
					}
				}
			}


		};
		updatable.regularly(updater);
		updatable.update();

		return updatable;
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

	private Updatable<Table> textSummary(Entity entity) {
		SkillsComponent skillsComponent = entity.getComponent(SkillsComponent.class);
		FactionComponent factionComponent = entity.getComponent(FactionComponent.class);

		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);

		Label headlineLabel = new Label("", managementSkin, "default-font-18-label");
		table.add(headlineLabel).left().spaceBottom(5f).row();

		Table behaviourTable = new Table();
		table.add(behaviourTable).grow();

		Runnable headlineLabelUpdater = () -> {
			headlineLabel.setText("");

			if (entity.getBehaviourComponent() instanceof CorpseBehaviour corpseBehaviour) {
				String deadText = corpseBehaviour.getDescription(i18nTranslator, gameContext, messageDispatcher)
						.stream()
						.map(I18nText::toString)
						.collect(Collectors.joining("\n"));
				headlineLabel.setText(deadText);
			} else if (factionComponent != null && (factionComponent.getFaction() == Faction.WILD_ANIMALS || factionComponent.getFaction() == Faction.MONSTERS)) {
				headlineLabel.setText(i18nTranslator.translate(factionComponent.getFaction().i18nKey));
			} else if (factionComponent != null && factionComponent.getFaction() == Faction.SETTLEMENT && !entity.isSettler()) {
				headlineLabel.setText(i18nTranslator.translate("FACTION.SETTLEMENT.ANIMAL"));
			} else if (skillsComponent != null && SettlerManagementScreen.IS_MILITARY.test(entity)) {
				String assignedWeaponText = settlerManagementScreen.getAssignedWeaponText(entity, skillsComponent);
				headlineLabel.setText(assignedWeaponText);
			} else if (skillsComponent != null && SettlerManagementScreen.IS_CIVILIAN.test(entity)) {
				java.util.List<SkillsComponent.QuantifiedSkill> activeProfessions = skillsComponent.getActiveProfessions();
				if (!activeProfessions.isEmpty()) {
					SkillsComponent.QuantifiedSkill quantifiedSkill = activeProfessions.get(0);
					headlineLabel.setText(i18nTranslator.getSkilledProfessionDescription(quantifiedSkill.getSkill(), quantifiedSkill.getLevel(),
							((CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getGender()).toString());
				}
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

		updatable.regularly(headlineLabelUpdater);
		updatable.regularly(descriptionUpdater);
		updatable.update();
		return updatable;
	}

	private Updatable<Table> debugTextSummary(Entity entity) {
		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);

		if (GlobalSettings.DEV_MODE) {
			Label.LabelStyle debugStyle = new Label.LabelStyle(managementSkin.get("default-font-18-label", Label.LabelStyle.class));
			debugStyle.fontColor = Color.PURPLE;
			updatable.regularly(() -> {
				table.clearChildren();

				if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
					if (creatureBehaviour.getCurrentGoal() != null) {
						String debugText = creatureBehaviour.getCurrentGoal().goal.name;
						if (creatureBehaviour.getCurrentGoal().getCurrentAction() != null) {
							debugText += ": " + creatureBehaviour.getCurrentGoal().getCurrentAction().getSimpleName();
						}
						table.add(new Label(debugText, debugStyle)).left().row();
					}
				}
			});
		}

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
			boolean unknownHappiness = isMilitary || !entity.isSettler() || entity.getBehaviourComponent() instanceof CorpseBehaviour; //Dead don't display happiness

			List<String> ailments = new ArrayList<>();
			for (StatusEffect statusEffect : statusComponent.getAll()) {
				if (statusEffect.getI18Key() != null) {
					ailments.add(i18nTranslator.translate(statusEffect.getI18Key()));
				}
			}
			for (I18nText damageDescription : attributes.getBody().getDamageDescriptions(i18nTranslator)) {
				ailments.add(damageDescription.toString());
			}

			if (!ailments.isEmpty()) {
				DecoratedString tooltipString = DecoratedString.fromString(ailments.get(0));
				for (int i = 1; i < ailments.size(); i++) {
					tooltipString = DecoratedString.of(tooltipString, DecoratedString.linebreak(), DecoratedString.fromString(ailments.get(i)));
				}
				DecoratedStringLabel tooltipContents = decoratedStringLabelFactory.create(tooltipString, "tooltip-text", mainGameSkin);

				Image injurySmiley = new Image(mainGameSkin.getInjuredSmiley(attributes, factionComponent));
				tooltipFactory.complexTooltip(injurySmiley, tooltipContents, TooltipFactory.TooltipBackground.LARGE_PATCH_LIGHT);
				table.add(injurySmiley);
			} else if (unknownHappiness) {
				Image notInjuredSmiley = new Image(mainGameSkin.getNotInjuredSmiley(attributes, factionComponent));
				tooltipFactory.simpleTooltip(notInjuredSmiley, "BODY_STRUCTURE.DAMAGE.NOT_INJURED", TooltipLocationHint.BELOW);
				table.add(notInjuredSmiley);
			}

			if (happinessComponent != null && isSettlement) {
				if (!unknownHappiness) {
					String drawableName = overallSmileyDrawable(happinessComponent.getNetModifier());
					Image smiley = new Image(mainGameSkin.getDrawable(drawableName));
					if (!happinessComponent.currentModifiers().isEmpty()) {
						tooltipFactory.complexTooltip(smiley, populateSmileyTooltip(entity, happinessComponent), TooltipFactory.TooltipBackground.LARGE_PATCH_LIGHT);
					}
					table.add(smiley);
				}
			}


		};
		updater.run();
		updatable.regularly(updater);
		return updatable;
	}

	private Table populateSmileyTooltip(Entity entity, HappinessComponent happinessComponent) {
		Table tooltipTable = new Table();
		boolean isNotDead = !(entity.getBehaviourComponent() instanceof CorpseBehaviour);
		if (happinessComponent != null && !SettlerManagementScreen.IS_MILITARY.test(entity) && isNotDead) {
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

			tooltipTable.clear();
			tooltipTable.add(infoContents);
		} else {
			tooltipTable.clear();
		}
		return tooltipTable;
	}

	private String smileyDrawable(int modifierAmount) {
		String drawableName;
		if (modifierAmount > 0) {
			drawableName = MainGameSkin.HAPPY;
		} else {
			drawableName = MainGameSkin.SAD;
		}
		return drawableName;
	}

	private String overallSmileyDrawable(int netModifierAmount) {
		String drawableName;
		if (netModifierAmount <= -43) {
			drawableName = MainGameSkin.MISERABLE;
		} else if (netModifierAmount <= -31) {
			drawableName = MainGameSkin.SAD;
		} else if (netModifierAmount <= -19) {
			drawableName = MainGameSkin.DOWN;
		} else if (netModifierAmount <= 0) {
			drawableName = MainGameSkin.NEUTRAL;
		} else if (netModifierAmount <= 12) {
			drawableName = MainGameSkin.CHEERY;
		} else if (netModifierAmount <= 24) {
			drawableName = MainGameSkin.JOLLY;
		} else if (netModifierAmount <= 36) {
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

	private Table squadEmblem(Entity entity) {
		Table table = new Table();
		if (SettlerManagementScreen.IS_MILITARY.test(entity)) {
			MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
			Squad squad = gameContext.getSquads().get(militaryComponent.getSquadId());
			if (squad != null) {
				Drawable emblem = managementSkin.getDrawable(managementSkin.getSmallEmblemName(squad));
				ImageButton button = new ImageButton(emblem);
				button.addListener(new ChangeCursorOnHover(button, GameCursor.SELECT, messageDispatcher));
				button.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						super.clicked(event, x, y);
						messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(squad));
					}
				});
				table.add(button);
			}
		}
		return table;
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
					textInputDialog.setMaxLength(MAX_DWARF_NAME_PLUS_15PC);
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
		updatable.regularly(() -> headerLabel.setText(attributes.getName().toString()));

		return updatable;
	}

	private Updatable<Table> inventory(Entity entity) {
		Table table = new Table();
		Updatable<Table> updatable = Updatable.of(table);
		InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
		DecorationInventoryComponent decorationInventoryComponent = entity.getComponent(DecorationInventoryComponent.class);

		final int MAX_SLOTS_PER_ROW = 8;
		updatable.regularly(() -> {
			table.clear();

			List<Entity> inventoryEntities = new ArrayList<>();
			AttachedEntitiesComponent attachedEntitiesComponent = entity.getComponent(AttachedEntitiesComponent.class);
			if (attachedEntitiesComponent != null) {
				inventoryEntities.addAll(attachedEntitiesComponent.getAttachedEntities().stream().map(entry -> entry.entity).toList());
			}
			if (entity.getComponent(HaulingComponent.class) != null && entity.getComponent(HaulingComponent.class).getHauledEntity() != null) {
				inventoryEntities.add(entity.getComponent(HaulingComponent.class).getHauledEntity());
			}
			EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);
			if (equippedItemComponent != null) {
				if (equippedItemComponent.getMainHandItem() != null) {
					inventoryEntities.add(equippedItemComponent.getMainHandItem());
				}
				if (equippedItemComponent.getOffHandItem() != null) {
					inventoryEntities.add(equippedItemComponent.getOffHandItem());
				}
				if (equippedItemComponent.getEquippedClothing() != null) {
					inventoryEntities.add(equippedItemComponent.getEquippedClothing());
				}
			}
			if (inventoryComponent != null) {
				inventoryEntities.addAll(inventoryComponent.getInventoryEntries().stream().map(entry -> entry.entity).toList());
			}
			if (decorationInventoryComponent != null) {
				inventoryEntities.addAll(decorationInventoryComponent.getDecorationEntities());
			}
			Iterator<Entity> inventoryIterator = inventoryEntities.iterator();

			for (int slotIndex = 1; slotIndex <= numTotalCells(MAX_SLOTS_PER_ROW, inventoryEntities); slotIndex++) {
				Stack entityStack = new Stack();

				Drawable emptyBackgroundDrawable = mainGameSkin.getDrawable("asset_dwarf_select_inventory_bg");

				Container<Image> backgroundContainer = new Container<>(new Image(emptyBackgroundDrawable));
				backgroundContainer.bottom().right();
				entityStack.add(backgroundContainer);

				if (inventoryIterator.hasNext()) {
					Entity inventoryItem = inventoryIterator.next();
					int quantity = 1;

					if (inventoryItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
						quantity = attributes.getQuantity();
					}
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
					table.add(entityStack).bottom().spaceLeft(22f).spaceTop(slotIndex > MAX_SLOTS_PER_ROW ? 22f : 0f);
				} else {
					table.add(entityStack).bottom().spaceLeft(22f).spaceTop(slotIndex > MAX_SLOTS_PER_ROW ? 22f : 0f);
				}
				if (slotIndex % MAX_SLOTS_PER_ROW == 0 && slotIndex < inventoryEntities.size()) {
					table.row();
				}
			}
		});

		updatable.update();
		return updatable;
	}

	private static int numTotalCells(int MAX_SLOTS_PER_ROW, List<Entity> inventoryEntities) {
		return ((inventoryEntities.size() / MAX_SLOTS_PER_ROW) + (inventoryEntities.size() % MAX_SLOTS_PER_ROW == 0 ? 0 : 1)) * MAX_SLOTS_PER_ROW;
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
