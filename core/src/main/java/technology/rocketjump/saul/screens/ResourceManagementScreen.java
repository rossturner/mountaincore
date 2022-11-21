package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.i18n.*;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MenuSkin;
import technology.rocketjump.saul.ui.widgets.EnhancedScrollPane;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;
import technology.rocketjump.saul.ui.widgets.LabelFactory;
import technology.rocketjump.saul.ui.widgets.ScaledToFitLabel;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.screens.ManagementScreenName.RESOURCES;
import static technology.rocketjump.saul.ui.i18n.I18nWordClass.PLURAL;

@Singleton
public class ResourceManagementScreen extends AbstractGameScreen implements GameContextAware, DisplaysText {
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final SettlementItemTracker settlementItemTracker;
	private final EntityRenderer entityRenderer;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final SoundAssetDictionary soundAssetDictionary;
	private final MenuSkin menuSkin;
	private final Skin mainGameSkin;
	private final Skin managementSkin;
	private final Drawable[] btnResourceItemVariants;
	private final Map<ItemQuality, Drawable> qualityStars;
	private final ScrollPane scrollPane;
	private final Image fullScreenOverlay;
	private final LabelFactory labelFactory;

	private Stack stack;
	private Actor currentInfoPane;
	private StockpileGroup selectedStockpileGroup;
	private Comparator<List<Entity>> selectedSortFunction;
	private Label stockpileGroupNameLabel;
	private String searchBarText = "";
	private ButtonGroup<Button> infoButtonGroup = new ButtonGroup<>();
	private GameContext gameContext;

	@Inject
	public ResourceManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                                I18nTranslator i18nTranslator, SettlementItemTracker settlementItemTracker,
	                                EntityRenderer entityRenderer, StockpileGroupDictionary stockpileGroupDictionary,
	                                SoundAssetDictionary soundAssetDictionary, LabelFactory labelFactory) {
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.settlementItemTracker = settlementItemTracker;
		this.entityRenderer = entityRenderer;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
		this.labelFactory = labelFactory;
		btnResourceItemVariants = new Drawable[]{
				managementSkin.getDrawable("btn_resources_item_01"),
				managementSkin.getDrawable("btn_resources_item_02"),
				managementSkin.getDrawable("btn_resources_item_03"),
				managementSkin.getDrawable("btn_resources_item_04")
		};
		this.qualityStars = Map.of(
				ItemQuality.AWFUL, managementSkin.getDrawable("asset_quality_star_01"),
				ItemQuality.POOR, managementSkin.getDrawable("asset_quality_star_02"),
				ItemQuality.STANDARD, managementSkin.getDrawable("asset_quality_star_03"),
				ItemQuality.SUPERIOR, managementSkin.getDrawable("asset_quality_star_04"),
				ItemQuality.MASTERWORK, managementSkin.getDrawable("asset_quality_star_05")
		);
		scrollPane = new EnhancedScrollPane(null, menuSkin);

		fullScreenOverlay = new Image(menuSkin, "default-rect");
		fullScreenOverlay.setFillParent(true);
		fullScreenOverlay.setColor(0, 0, 0, 0.6f);
		fullScreenOverlay.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				removeInfoPane();
			}
		});
	}

	@Override
	public String getName() {
		return RESOURCES.name();
	}

	//Screen implementation
	@Override
	public void show() {
		clearContextRelatedState();

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new ManagementScreenInputHandler(messageDispatcher));
		Gdx.input.setInputProcessor(inputMultiplexer);
		rebuildUI();
		stage.setKeyboardFocus(null);
	}

	@Override
	public void dispose() { }

	//Game Context implementation
	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		this.selectedSortFunction = null;
	}

	//Called from Screen.show() and elsewhere when language changes
	@Override
	public void rebuildUI() {
		infoButtonGroup.clear();
		infoButtonGroup.setMinCheckCount(0);

		stack = new Stack();
		stack.setFillParent(true);
		stack.add(menuSkin.buildBackgroundBaseLayer());
		stack.add(menuSkin.buildPaperLayer(buildPaperComponents()));
		stack.add(buildExitTable());

		stage.addActor(stack);
	}

	private Actor buildExitTable() {
		Table table = new Table();
		Button exitButton = new Button(menuSkin, "btn_exit");
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
			}
		});
		attachClickCursor(exitButton, GameCursor.SELECT);
		table.add(exitButton).expandX().align(Align.topLeft).padLeft(257 + menuSkin.getDrawable("paper_texture_bg_pattern_large").getMinWidth() + 5f).padTop(5f).row();
		table.add().grow();
		return table;
	}

	private Table buildPaperComponents() {
		Label titleLabel = labelFactory.titleRibbon("GUI.RESOURCE_MANAGEMENT.TITLE");

		stockpileGroupNameLabel = new Label("", managementSkin, "stockpile_group_filter_label"); //probably should be scaled to fit label
		stockpileGroupNameLabel.setAlignment(Align.left);

		Table stockpileButtons = new Table();
		ButtonGroup<ImageButton> stockpileButtonGroup = new ButtonGroup<>();
		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll()) {
			String drawableName = stockpileGroup.getDrawableName();
			ImageButton.ImageButtonStyle clonedStyle = new ImageButton.ImageButtonStyle(menuSkin.get("default", ImageButton.ImageButtonStyle.class));
			clonedStyle.imageUp = mainGameSkin.getDrawable(drawableName);
			ImageButton stockpileButton = new ImageButton(clonedStyle);

			stockpileButtonGroup.add(stockpileButton);
			stockpileButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (stockpileButton.isChecked()) {
						removeInfoPane();

						selectedStockpileGroup = stockpileGroup;
						rebuildStockpileComponents();
					}
				}
			});
			attachClickCursor(stockpileButton, GameCursor.SELECT);

			stockpileButtons.add(stockpileButton).padLeft(2f).padRight(2f);
			if (stockpileGroup.equals(selectedStockpileGroup)) {
				stockpileButton.setChecked(true);
			}
		}
		if (selectedStockpileGroup == null) {
			stockpileButtonGroup.getButtons().get(0).setChecked(true);
			selectedStockpileGroup = stockpileGroupDictionary.getAll().get(0);
		}


		TextField searchBar = new TextField("", managementSkin, "search_bar_input");
		searchBar.setText(searchBarText);
		searchBar.setMessageText(i18nTranslator.translate("GUI.RESOURCE_MANAGEMENT.SEARCH"));
		searchBar.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if (event.getKeyCode() == Input.Keys.ESCAPE) {
					stage.setKeyboardFocus(null);
					return false;
				}
				return super.keyDown(event, keycode);
			}

			@Override
			public boolean keyTyped(InputEvent event, char character) {
				searchBarText = searchBar.getText();
				rebuildStockpileComponents();
				return true;
			}
		});
		attachClickCursor(searchBar, GameCursor.I_BEAM);
		Label sortByLabel  = new Label(i18nTranslator.translate("GUI.RESOURCE_MANAGEMENT.SORT_BY"), managementSkin, "sort_by_label");

		Comparator<List<Entity>> quantityComparator = Comparator.comparing((Function<List<Entity>, Integer>) entities -> groupSum(entities, entity -> ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getQuantity())).reversed();
		Comparator<List<Entity>> availabilityComparator = Comparator.comparing((Function<List<Entity>, Integer>) entities -> groupSum(entities, entity -> entity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated())).reversed();
		Comparator<List<Entity>> goldComparator = Comparator.comparing((Function<List<Entity>, Integer>) entities -> 0).reversed();
		Button sortByTotal = buildTextSortButton("GUI.RESOURCE_MANAGEMENT.TOTAL", quantityComparator);
		Button sortByAvailability = buildTextSortButton("GUI.RESOURCE_MANAGEMENT.AVAILABLE", availabilityComparator);
		Button sortByGold = buildIconSortButton("icon_coin", goldComparator);
		ButtonGroup<Button> buttonGroup = new ButtonGroup<>(sortByTotal, sortByAvailability, sortByGold);
		buttonGroup.setMinCheckCount(0);

		Table filters = new Table();
		filters.defaults().growX();
		filters.add(stockpileGroupNameLabel).width(400f).padLeft(8f);
		filters.add(searchBar).width(524);
		filters.add(sortByLabel);
		filters.add(sortByGold);
		filters.add(sortByAvailability);
		filters.add(sortByTotal);

		rebuildStockpileComponents();
		scrollPane.setForceScroll(false, true);
		scrollPane.setFadeScrollBars(false);
		scrollPane.setScrollbarsVisible(true);
		scrollPane.setScrollBarPositions(true, true);

		Table mainTable = new Table();
		mainTable.add(stockpileButtons).row();
		mainTable.add(filters).growX().row();
		mainTable.add(new Image(managementSkin.getDrawable("asset_resources_line"))).row();
		mainTable.add(scrollPane).height(1426).grow();

		Table table = new Table();
		table.add(titleLabel).padTop(54f).row();
		table.add(mainTable).padLeft(38f).padRight(38f).spaceTop(48f).fill().growY();
		return table;
	}


	//when row is selected for info
	private void rebuildInfoComponents(List<Entity> group, Function<Entity, String> displayNameFunction) {
		Drawable assetMoreInfoBgStrip = managementSkin.getDrawable("asset_more_info_bg_10patch");
		float wrappedLabelWidth = assetMoreInfoBgStrip.getMinWidth() - assetMoreInfoBgStrip.getLeftWidth() - assetMoreInfoBgStrip.getRightWidth();
		Entity exampleEntity = group.get(0);
		Label infoTitle = new ScaledToFitLabel(displayNameFunction.apply(exampleEntity), menuSkin, "secondary_banner_title", 500);
		infoTitle.setAlignment(Align.center);


		Table entitiesTable = new Table();
		entitiesTable.top();
		for (Entity entity : group) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			ItemAllocationComponent itemAllocationComponent = entity.getComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent != null && itemAllocationComponent.getNumAllocated() > 0) {
				for (ItemAllocation itemAllocation : itemAllocationComponent.getAll()) {
					Gender gender = Gender.ANY;
					I18nWordClass wordClass = I18nWordClass.UNSPECIFIED;
					int allocationAmount = itemAllocation.getAllocationAmount();
					if (allocationAmount > 1) {
						wordClass = PLURAL;
					}
					Long owningEntityId = itemAllocation.getOwningEntityId();
					final Entity owningEntity;
					if (owningEntityId != null) {
						owningEntity = gameContext.getEntities().get(owningEntityId);
						if (owningEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes) {
							gender =  creatureEntityAttributes.getGender();
						}
					} else {
						owningEntity = null;
					}
					String itemName = i18nTranslator.getItemDescription(allocationAmount, attributes.getPrimaryMaterial(), attributes.getItemType(), attributes.getItemQuality()).toString();

					String itemAllocationLabelText = itemName;
					if (itemAllocation.getPurpose() != null) {
						I18nWord word = i18nTranslator.getDictionary().getWord("GUI.RESOURCE_MANAGEMENT.ITEM_ALLOCATION_PURPOSE." + itemAllocation.getPurpose().name());
						itemAllocationLabelText = i18nTranslator.applyReplacements(word, Map.of("item", new I18nWord(itemName)), wordClass, gender).toString();
					}







					Label itemAllocationLabel = new Label(itemAllocationLabelText, managementSkin, "table_value_label");
					itemAllocationLabel.setWrap(true);
					itemAllocationLabel.setAlignment(Align.center);

					entitiesTable.add(itemAllocationLabel).width(wrappedLabelWidth-50f).row();


					if (owningEntity != null) {
						String owningEntityDescription = i18nTranslator.getDescription(owningEntity).toString();

						TextButton gotoOwnerButton = new TextButton(owningEntityDescription, managementSkin, "goto_dwarf_button");
						attachClickCursor(gotoOwnerButton, GameCursor.SELECT);
						gotoOwnerButton.addListener(new ClickListener() {
							@Override
							public void clicked(InputEvent event, float x, float y) {
								super.clicked(event, x, y);
								gotoEntity(owningEntity);
							}
						});

						entitiesTable.add(gotoOwnerButton).growX().padTop(30).padBottom(100).padRight(32).padLeft(48).row();
					}
				}
			}
		}
		entitiesTable.add().growY();


		ScrollPane entitiesScrollpane = new EnhancedScrollPane(entitiesTable, menuSkin);
		entitiesScrollpane.setForceScroll(false, true);
		entitiesScrollpane.setFadeScrollBars(false);
		entitiesScrollpane.setScrollbarsVisible(true);
		entitiesScrollpane.setScrollBarPositions(true, true);
		Table sideTable = new Table();
		sideTable.setBackground(assetMoreInfoBgStrip);

		Button exitButton = new Button(menuSkin, "btn_exit");
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				removeInfoPane();
			}
		});
		attachClickCursor(exitButton, GameCursor.SELECT);
		sideTable.add(exitButton).expandX().align(Align.topLeft).pad(5f).row();
		sideTable.add(infoTitle).width(wrappedLabelWidth).padBottom(100).row();
		sideTable.add(entitiesScrollpane).growY().row();

		Table table = new Table();
		table.add().grow();
		table.add(sideTable).right().growY().padRight(200f);
		this.currentInfoPane = table;

		stack.add(fullScreenOverlay);
		stack.add(currentInfoPane);
	}

	private void removeInfoPane() {
		stack.removeActor(currentInfoPane);
		stack.removeActor(fullScreenOverlay);
	}

	//When stockpile group selection changes, or any filter/sorts change below
	private void rebuildStockpileComponents() {
		stockpileGroupNameLabel.setText(i18nTranslator.translate(selectedStockpileGroup.getI18nKey()));
		scrollPane.setActor(buildItemsTable());
	}

	private Table buildItemsTable() {
		Table itemsTable = new Table();
		itemsTable.align(Align.top);

		Function<Entity, String> levelOneDisplayName = entity -> {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			I18nText levelOneName = i18nTranslator.getTranslatedString(attributes.getItemType().getI18nKey(), PLURAL);

			return i18nTranslator.getTranslatedWordWithReplacements("GUI.RESOURCE_MANAGEMENT.ALL_OF", Map.of("item", levelOneName)).toString();

		};

		Function<Entity, String> levelTwoDisplayName = entity -> {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			I18nText levelTwoName = i18nTranslator.getItemDescription(Integer.MAX_VALUE, attributes.getPrimaryMaterial(), attributes.getItemType(), attributes.getItemQuality());
			return i18nTranslator.getTranslatedWordWithReplacements("GUI.RESOURCE_MANAGEMENT.ALL_OF", Map.of("item", levelTwoName)).toString();
		};

		Function<Entity, String> levelThreeDisplayName = entity -> {
			return i18nTranslator.getDescription(entity).toString();
		};

		Function<Entity, String> levelOneGroup = entity -> {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return attributes.getItemType().getItemTypeName();
		};

		Function<Entity, String> levelTwoGroup = entity -> {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return levelOneGroup.apply(entity) + ":" + attributes.getPrimaryMaterial().getMaterialName() + ":" + attributes.getItemQuality();
		};

		Function<Entity, String> levelThreeGroup = entity -> {
			return levelTwoGroup.apply(entity) + ":" + entity.getId();
		};

		List<Function<Entity, String>> groupings = List.of(levelOneGroup, levelTwoGroup, levelThreeGroup);
		List<Function<Entity, String>> displayNameFunctions = List.of(levelOneDisplayName, levelTwoDisplayName, levelThreeDisplayName);

		List<Entity> allEntities = settlementItemTracker.getAllByItemType()
				.values().stream()
				.flatMap(it -> it.values().stream())
				.flatMap(it -> it.values().stream())
				.filter(entity -> {
					ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					StockpileGroup stockpileGroup = attributes.getItemType().getStockpileGroup();
					return stockpileGroup != null && stockpileGroup.equals(selectedStockpileGroup);
				})
				.filter(entity -> {
					if (searchBarText.isEmpty()) {
						return true;
					} else {
						for (Function<Entity, String> nameFunction : displayNameFunctions) {
							String displayName = nameFunction.apply(entity);
							if (displayName.toLowerCase().contains(searchBarText.toLowerCase())) {
								return true;
							}
						}
						return false;
					}
				})
				.collect(Collectors.toList());


		//Rocky: Dislike this code underneath, feels mad
		Comparator<List<Entity>> reverseQualityOrder = Comparator.comparing((Function<List<Entity>, Integer>) entitiesToSort ->  {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entitiesToSort.get(0).getPhysicalEntityComponent().getAttributes();
			return attributes.getItemQuality().ordinal();
		}).reversed();

		Comparator<List<Entity>> levelTwoSort = Comparator.comparing((Function<List<Entity>, String>) entitiesToSort ->  {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entitiesToSort.get(0).getPhysicalEntityComponent().getAttributes();
			return i18nTranslator.getItemDescription(1, attributes.getPrimaryMaterial(), attributes.getItemType(), null).toString();
		}).thenComparing(reverseQualityOrder);
		List<Comparator<List<Entity>>> defaultSorts = List.of(
				Comparator.comparing(entitiesToSort -> levelOneDisplayName.apply(entitiesToSort.get(0))),
				levelTwoSort,
				Comparator.comparing(entitiesToSort -> levelThreeDisplayName.apply(entitiesToSort.get(0)))
		);

		recursivelyAdd(itemsTable, allEntities, groupings, 0, displayNameFunctions, defaultSorts);

		return itemsTable;
	}

	private int groupSum(List<Entity> entities, ToIntFunction<Entity> property) {
		return entities.stream().mapToInt(property).sum();
	}

	private void recursivelyAdd(Table parent, Collection<Entity> entities, List<Function<Entity, String>> groupings, int groupingIndex, List<Function<Entity, String>> displayNameFunctions,
	                            List<Comparator<List<Entity>>> defaultSorts) {
		if (groupingIndex != groupings.size()) {
			Function<Entity, String> displayNameFunction = displayNameFunctions.get(groupingIndex);
			Function<Entity, String> groupFunction = groupings.get(groupingIndex);
			Map<String, List<Entity>> groupedEntities = entities.stream().collect(Collectors.groupingBy(groupFunction));

			Comparator<List<Entity>> sortToUse = selectedSortFunction;

			if (sortToUse == null) {
				sortToUse =  defaultSorts.get(groupingIndex);
			}

			for (List<Entity> group : groupedEntities.values().stream().sorted(sortToUse).toList()) {
				//aggregate stats
				int totalQuantity = groupSum(group, entity -> ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getQuantity());
				int totalUnallocated = groupSum(group, entity -> entity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated());
				int totalGold = 0; //todo: semi yagni, fill me when we do trading
				ItemQuality itemQuality = null;
				Entity exampleEntity = group.get(0);
				for (Entity entity : group) {
					itemQuality =  ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getItemQuality();
				}



				Label entityLabel = new Label(displayNameFunction.apply(exampleEntity), managementSkin, "item_type_name_label");
				entityLabel.setWrap(true);
				entityLabel.setAlignment(Align.center);
				Table exampleEntityColumn = new Table();
				exampleEntityColumn.add(buildEntityButton(exampleEntity, totalQuantity)).size(205).row();
				exampleEntityColumn.add(entityLabel).width(240);

				HorizontalGroup itemTypeGoldGroup = new HorizontalGroup();
				itemTypeGoldGroup.space(managementSkin.getFont("default-font-24").getSpaceXadvance());
				itemTypeGoldGroup.addActor(new Label(String.valueOf(totalGold), managementSkin, "table_value_label"));
				itemTypeGoldGroup.addActor(new Image(managementSkin, "icon_coin"));
				itemTypeGoldGroup.align(Align.right);

				I18nText availableOfTotal = i18nTranslator.getTranslatedWordWithReplacements("GUI.RESOURCE_MANAGEMENT.AVAILABLE_OF_TOTAL",
						Map.of("available", new I18nWord(String.valueOf(totalUnallocated)),
								"total", new I18nWord(String.valueOf(totalQuantity))));
				HorizontalGroup itemTypeAvailableGroup = new HorizontalGroup();
				itemTypeAvailableGroup.space(managementSkin.getFont("default-font-24").getSpaceXadvance());
				itemTypeAvailableGroup.addActor(new Label(availableOfTotal.toString(), managementSkin, "table_value_label"));

				Button infoButton = new Button(managementSkin, "info_button");
				itemTypeAvailableGroup.addActor(infoButton);
				itemTypeAvailableGroup.align(Align.right);

				infoButtonGroup.add(infoButton);

				if (totalUnallocated != totalQuantity) {
					infoButton.addListener(new ClickListener() {
						@Override
						public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
							event.cancel(); //prevent propagation to the table row's clicked
						}

						@Override
						public void clicked(InputEvent event, float x, float y) {
							event.cancel();
						}
					});

					infoButton.addListener(new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							removeInfoPane();

							if (infoButton.isChecked()) {
								rebuildInfoComponents(group, displayNameFunction);
							}

							event.stop();
						}
					});
					attachClickCursor(infoButton, GameCursor.SELECT);
				} else {
					infoButton.setVisible(false);
				}

				Image qualityImage = new Image(qualityStars.get(itemQuality));
				Container<Image> qualityImageContainer = new Container<>(qualityImage);

				Table itemRow = new Table();
				itemRow.add(exampleEntityColumn).left().padLeft(100 * groupingIndex).growX();
				itemRow.add(qualityImageContainer).width(300);
				//this is a fudge as quality doesn't appear on first row
				if (groupingIndex == 0) {
					qualityImage.setVisible(false);
				}
				itemRow.add(itemTypeGoldGroup).right().width(400);
				itemRow.add(itemTypeAvailableGroup).right().padRight(50).width(800);
				parent.add(itemRow).padTop(40).padBottom(50).right().growX().row();


				Table childTable = new Table();
				childTable.setFillParent(true);
				recursivelyAdd(childTable, group, groupings, groupingIndex+1, displayNameFunctions, defaultSorts);

				itemRow.setTouchable(Touchable.enabled);
				itemRow.addListener(new InputListener() {
					@Override
					public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
						itemRow.setBackground(managementSkin.getDrawable("accent_bg"));
					}

					@Override
					public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
						itemRow.setBackground((Drawable) null);
					}
				});
				attachClickCursor(itemRow, GameCursor.SELECT);

				if (childTable.hasChildren()) {
					CollapsibleWidget collapsibleWidget = new CollapsibleWidget(childTable);
					collapsibleWidget.setCollapsed(searchBarText.isEmpty(), false);
					collapsibleWidget.addListener(new InputListener() {
						@Override
						public boolean keyUp(InputEvent event, int keycode) {
							if (event.getKeyCode() == Input.Keys.ESCAPE) {
								return false;
							}
							return super.keyUp(event, keycode);
						}
					});
					itemRow.addListener(new ClickListener() {
						@Override
						public void clicked(InputEvent event, float x, float y) {
							if (event.isCancelled()) {
								return;
							}

							collapsibleWidget.setCollapsed(!collapsibleWidget.isCollapsed(), false);
						}
					});
					parent.add(collapsibleWidget).growX().row();
				} else {
					itemRow.addListener(new ClickListener() {
						@Override
						public void clicked(InputEvent event, float x, float y) {
							if (event.isCancelled()) {
								return;
							}

							Entity target = exampleEntity;
							gotoEntity(target);
						}
					});
				}
			}
		}
	}

	private void gotoEntity(Entity target) {
		while (target.getLocationComponent().getContainerEntity() != null) {
			target = target.getLocationComponent().getContainerEntity();
		}
		Vector2 position = target.getLocationComponent().getWorldOrParentPosition();

		if (position != null) {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
			messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, position);
			messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(target, 0));
		} else {
			Logger.error("Attempting to move to entity with no position or container");
		}
	}

	private Drawable bgForExampleEntity(long entityId) {
		return btnResourceItemVariants[(int) (entityId % btnResourceItemVariants.length)];
	}

	private Button buildIconSortButton(String drawableName, Comparator<List<Entity>> sortFunction) {
		Image icon = new Image(managementSkin.getDrawable(drawableName));
		ImageTextButton button = new ImageTextButton("", managementSkin, "sort_by_button");
		button.defaults().padRight(9f);
		button.add(icon);
		Image image = button.getImage(); //Swap actors or cells doesn't work, absolute agony
		button.removeActor(image);
		button.add(image);
		button.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (button.isChecked()) {
					selectedSortFunction = sortFunction;
				} else {
					selectedSortFunction = null;
				}
				rebuildStockpileComponents();
			}
		});
		attachClickCursor(button, GameCursor.SELECT);
		return button;
	}

	private Button buildTextSortButton(String i18nKey, Comparator<List<Entity>> sortFunction) {
		ImageTextButton button = new ImageTextButton(i18nTranslator.translate(i18nKey), managementSkin, "sort_by_button");
		button.defaults().padRight(9f).spaceLeft(12f);
		Image image = button.getImage(); //Swap actors or cells doesn't work, absolute agony
		button.removeActor(image);
		button.add(image);
		button.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (button.isChecked()) {
					selectedSortFunction = sortFunction;
				} else {
					selectedSortFunction = null;
				}
				rebuildStockpileComponents();
			}
		});
		attachClickCursor(button, GameCursor.SELECT);
		return button;
	}

	private void attachClickCursor(Actor actor, GameCursor gameCursor) {
		actor.addListener(new ChangeCursorOnHover(actor, gameCursor, messageDispatcher));
		actor.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
	}

	private Actor buildEntityButton(Entity exampleEntity, int quantity) {
		Stack entityStack = new Stack();

		Drawable btnResourceItemBg = bgForExampleEntity(exampleEntity.getId());
		Button itemTypeButton = new Button(new EntityDrawable(
				exampleEntity, entityRenderer, true, messageDispatcher
		).withBackground(btnResourceItemBg));

		Label amountLabel = new Label(String.valueOf(quantity), managementSkin, "entity_drawable_quantity_label");
		amountLabel.setAlignment(Align.center);

		Table amountTable = new Table();
		amountTable.add(amountLabel).left().top();
		amountTable.add(new Container<>()).width(btnResourceItemBg.getMinWidth()-32f).expandX().row();
		amountTable.add(new Container<>()).colspan(2).height(btnResourceItemBg.getMinHeight()-32f).expandY();

		entityStack.add(itemTypeButton);
		if (quantity > 1) {
			entityStack.add(amountTable);
		}

		return entityStack;
	}
}
