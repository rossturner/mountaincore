package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.EnhancedScrollPane;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;
import technology.rocketjump.saul.ui.widgets.GameDialog;
import technology.rocketjump.saul.ui.widgets.ScaledToFitLabel;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static technology.rocketjump.saul.rendering.camera.DisplaySettings.GUI_DESIGN_SIZE;
import static technology.rocketjump.saul.screens.ManagementScreenName.RESOURCES;

@Singleton
public class ResourceManagementScreen implements GameScreen, GameContextAware, DisplaysText {
	private final Stage stage;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final SettlementItemTracker settlementItemTracker;
	private final EntityRenderer entityRenderer;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final OrthographicCamera camera = new OrthographicCamera();
	private final Skin menuSkin;
	private final Skin mainGameSkin;
	private final Skin managementSkin;

	private StockpileGroup selectedStockpileGroup;
	private final RandomXS128 random = new RandomXS128();
	private final Drawable[] btnResourceItemVariants;
	private final Map<ItemQuality, Drawable> qualityStars;
	private final ScrollPane scrollPane;
//	private final Table stockpileListing = new Table(); //to be cleared and repopulated on filter changes


	@Inject
	public ResourceManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
									I18nTranslator i18nTranslator, SettlementItemTracker settlementItemTracker,
									EntityRenderer entityRenderer, StockpileGroupDictionary stockpileGroupDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.settlementItemTracker = settlementItemTracker;
		this.entityRenderer = entityRenderer;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.stage = new Stage(new ExtendViewport(GUI_DESIGN_SIZE.x, GUI_DESIGN_SIZE.y));
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();
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
	}

	@Override
	public String getName() {
		return RESOURCES.name();
	}

	@Override
	public void showDialog(GameDialog dialog) {
		//Does nothing, no dialog spawned from this screen so far
	}

	//Screen impls
	@Override
	public void show() {
		clearContextRelatedState();

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new ManagementScreenInputHandler(messageDispatcher));
		Gdx.input.setInputProcessor(inputMultiplexer);
		rebuildUI();
	}

	@Override
	public void render(float delta) {
		camera.update();
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		camera.setToOrtho(false, width, height);
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void pause() { }

	@Override
	public void resume() { }

	@Override
	public void hide() { }

	@Override
	public void dispose() { }

	//Game Context impls
	@Override
	public void onContextChange(GameContext gameContext) {
	}

	@Override
	public void clearContextRelatedState() {

	}

	//Called from Screen.show() and elsewhere when language changes
	@Override
	public void rebuildUI() {
		Stack stack = new Stack();
		stack.setFillParent(true);
		stack.add(buildBackgroundBaseLayer());
		stack.add(buildPaperLayer());

		stage.addActor(stack);
	}
	//copied from PaperMenu
	private Actor buildBackgroundBaseLayer() {
		Table table = new Table();
		table.setName("backgroundBase");
		table.add(new Image(menuSkin.getDrawable("menu_bg_left"))).left();
		table.add().expandX();
		table.add(new Image(menuSkin.getDrawable("menu_bg_right"))).right();
		return table;
	}

	private Table buildPaperLayer() {
		Table baseLayer = new Table();
		baseLayer.setBackground(menuSkin.getDrawable("paper_texture_bg"));
		baseLayer.add(new Image(menuSkin.getDrawable("paper_texture_bg_pattern_large"))).growY().padLeft(257);
		baseLayer.add(buildPaperComponents()).expandX();
		baseLayer.add(new Image(menuSkin.getDrawable("paper_texture_bg_pattern_large"))).growY().padRight(257);
		return baseLayer;
	}

	private Table buildPaperComponents() {
		Label titleLabel = new Label(translate("GUI.RESOURCE_MANAGEMENT.TITLE"), menuSkin, "title_ribbon");
		titleLabel.setAlignment(Align.center);

		Table stockpileButtons = new Table();
		ButtonGroup<ImageButton> stockpileButtonGroup = new ButtonGroup<>();
		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll()) {
			String drawableName = stockpileGroup.getDrawableName();
			ImageButton.ImageButtonStyle clonedStyle = new ImageButton.ImageButtonStyle(menuSkin.get("default", ImageButton.ImageButtonStyle.class));
			clonedStyle.imageUp = mainGameSkin.getDrawable(drawableName);
			ImageButton stockpileButton = new ImageButton(clonedStyle);
			stockpileButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (stockpileButton.isChecked()) {
						selectedStockpileGroup = stockpileGroup;
						rebuildStockpileComponents();
						//TODO Fill me
					}
				}
			});

			stockpileButtonGroup.add(stockpileButton);
			stockpileButtons.add(stockpileButton).padLeft(2f).padRight(2f);
		}

		Label stockpileGroupNameLabel = new Label(translate(selectedStockpileGroup.getI18nKey()), managementSkin, "stockpile_group_filter_label"); //probably should be scaled to fit label
		TextField searchBar = new TextField("", managementSkin, "search_bar_input");
		searchBar.setMessageText(translate("GUI.RESOURCE_MANAGEMENT.SEARCH"));
		searchBar.addListener(new InputListener() {
			@Override
			public boolean keyTyped(InputEvent event, char character) {

				return true;
			}
		});
		Label sortByLabel  = new Label(translate("GUI.RESOURCE_MANAGEMENT.SORT_BY"), managementSkin, "sort_by_label");

		Button sortByTotal = buildTextSortButton("GUI.RESOURCE_MANAGEMENT.TOTAL");
		Button sortByAvailability = buildTextSortButton("GUI.RESOURCE_MANAGEMENT.AVAILABLE");
		Button sortByGold = buildIconSortButton("icon_coin");
		Button sortByQuality = buildIconSortButton("asset_quality_star_01");
		ButtonGroup<Button> buttonGroup = new ButtonGroup<>(sortByTotal, sortByAvailability, sortByGold, sortByQuality);
		buttonGroup.setMinCheckCount(0);

		Table filters = new Table();
		filters.defaults().growX();
		filters.add(stockpileGroupNameLabel);
		filters.add(searchBar).width(524);
		filters.add(sortByLabel);
		filters.add(sortByTotal);
		filters.add(sortByAvailability);
		filters.add(sortByGold);
		filters.add(sortByQuality);


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

	//When stockpile group selection changes, or any filter/sorts change below
	private void rebuildStockpileComponents() {
		scrollPane.setActor(buildItemsTable());
	}


	private Table buildItemsTable() {
		Table itemsTable = new Table();
		itemsTable.align(Align.top);

		Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> filteredByStockpileGroup = new LinkedHashMap<>();
		Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> allByItemType = settlementItemTracker.getAllByItemType();
		for (Map.Entry<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemTypeMapEntry : allByItemType.entrySet()) {
			StockpileGroup itemStockpileGroup = itemTypeMapEntry.getKey().getStockpileGroup();
			if (itemStockpileGroup != null && itemStockpileGroup.equals(selectedStockpileGroup)) {
				filteredByStockpileGroup.put(itemTypeMapEntry.getKey(), itemTypeMapEntry.getValue());
			}
		}


		for (Map.Entry<ItemType, Map<GameMaterial, Map<Long, Entity>>> entry : filteredByStockpileGroup.entrySet()) {
			ItemType itemType = entry.getKey();
			Map<GameMaterial, Map<Long, Entity>> byGameMaterial = entry.getValue();
			int totalQuantity = 0;
			int totalUnallocated = 0;
			int totalGold = 0; //todo: semi yagni, fill me when we do trading
			for (Map<Long, Entity> entityMap : byGameMaterial.values()) {
				for (Entity itemEntity : entityMap.values()) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
					totalQuantity += attributes.getQuantity();
					totalUnallocated += itemEntity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
				}
			}

			Entity exampleEntity = byGameMaterial.values().iterator().next().values().iterator().next();
			Drawable btnResourceItemBg = randomBtnResourceItemBg();
			Button itemTypeButton = new Button(new EntityDrawable(
					exampleEntity, entityRenderer, true, messageDispatcher
			).withBackground(btnResourceItemBg));

			Label itemTypeNameLabel = new ScaledToFitLabel(translate(itemType.getI18nKey()), managementSkin, "item_type_name_label", 205);
			itemTypeNameLabel.setAlignment(Align.center);
			Table itemTypeColumn = new Table();
			itemTypeColumn.add(itemTypeButton).size(205).row();
			itemTypeColumn.add(itemTypeNameLabel);

			HorizontalGroup itemTypeGoldGroup = buildMeasureLabel("GUI.RESOURCE_MANAGEMENT.TOTAL", totalGold);
			itemTypeGoldGroup.addActorAt(1, new Image(managementSkin, "icon_coin"));
			HorizontalGroup itemTypeQuantityGroup = buildMeasureLabel("GUI.RESOURCE_MANAGEMENT.TOTAL", totalQuantity);
			HorizontalGroup itemTypeAvailableGroup = buildMeasureLabel("GUI.RESOURCE_MANAGEMENT.AVAILABLE", totalUnallocated);

			Table itemTypeTable = new Table();
			itemTypeTable.defaults().growX();
			itemTypeTable.add(itemTypeColumn);
			itemTypeTable.add(itemTypeGoldGroup);
			itemTypeTable.add(itemTypeQuantityGroup);
			itemTypeTable.add(itemTypeAvailableGroup);
			itemTypeTable.row();

			CollapsibleWidget collapsibleWidget = new CollapsibleWidget(addMaterialAndQualityRows(byGameMaterial, itemType));
			collapsibleWidget.setCollapsed(true);
			itemTypeTable.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					collapsibleWidget.setCollapsed(!collapsibleWidget.isCollapsed(), false);
				}
			});
			itemTypeTable.setTouchable(Touchable.enabled);

			itemsTable.add(itemTypeTable).growX().padTop(40).padBottom(50).row();
			itemsTable.add(collapsibleWidget).growX().row();
			itemsTable.debug();
		}

		return itemsTable;
	}

	private Table addMaterialAndQualityRows(Map<GameMaterial, Map<Long, Entity>> byGameMaterial, ItemType itemType) {
		Table nestedTable = new Table();
		nestedTable.setFillParent(true);
		//TODO: not keen on the maps of maps, makes this harder to read
		for (GameMaterial material : byGameMaterial.keySet()) {
			Map<ItemQuality,  Map<Long, Entity>> byItemQuality = new HashMap<>();
			for (Entity entity : byGameMaterial.get(material).values()) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (!byItemQuality.containsKey(attributes.getItemQuality())) {
					byItemQuality.put(attributes.getItemQuality(), new HashMap<>());
				}
				byItemQuality.get(attributes.getItemQuality()).put(entity.getId(), entity);
			}

			for (Map.Entry<ItemQuality, Map<Long, Entity>> entry : byItemQuality.entrySet()) {
				//TODO: mostly a copy and paste
				int totalGold = 0;
				int totalQuantity = 0;
				int totalUnallocated = 0;
				Entity exampleEntity = null;
				for (Entity entity : entry.getValue().values()) {
					exampleEntity = entity;
					ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					totalQuantity += attributes.getQuantity();
					totalUnallocated += entity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
				}

				Drawable btnResourceItemBg = randomBtnResourceItemBg();
				Button itemTypeButton = new Button(new EntityDrawable(
						exampleEntity, entityRenderer, true, messageDispatcher
				).withBackground(btnResourceItemBg));


				I18nText materialDescription = i18nTranslator.getItemDescription(1, material, itemType, null);

				Label itemTypeNameLabel = new ScaledToFitLabel(materialDescription.toString(), managementSkin, "item_type_name_label", 205);
				itemTypeNameLabel.setAlignment(Align.center);
				Table itemTypeColumn = new Table();
				itemTypeColumn.add(itemTypeButton).size(205).row();
				itemTypeColumn.add(itemTypeNameLabel);

				Image qualityImage = new Image(qualityStars.get(entry.getKey()));
//				qualityImage.setWidth(192);
//				qualityImage.setHeight(92);

				HorizontalGroup itemTypeGoldGroup = buildMeasureLabel("GUI.RESOURCE_MANAGEMENT.TOTAL", totalGold);
				itemTypeGoldGroup.addActorAt(1, new Image(managementSkin, "icon_coin"));
				itemTypeGoldGroup.removeActorAt(0, false);//fudge to remove the Total
				HorizontalGroup itemTypeQuantityGroup = buildMeasureLabel("GUI.RESOURCE_MANAGEMENT.TOTAL", totalQuantity);
				HorizontalGroup itemTypeAvailableGroup = buildMeasureLabel("GUI.RESOURCE_MANAGEMENT.AVAILABLE", totalUnallocated);

				Table itemTypeTable = new Table();
				itemTypeTable.defaults().growX();
				itemTypeTable.add(itemTypeColumn);
				itemTypeTable.add(qualityImage).fill(false, false);
				itemTypeTable.add(itemTypeGoldGroup);
				itemTypeTable.add(itemTypeQuantityGroup);
				itemTypeTable.add(itemTypeAvailableGroup);

//				todo: add hover accent
//				mainTable.setBackground(managementSkin.getDrawable("accent_bg"));
				nestedTable.add(itemTypeTable).padLeft(50f).padBottom(100f).growX().row();
			}
		}
		return nestedTable;
	}

	private HorizontalGroup buildMeasureLabel(String i18nKey, int value) {
		HorizontalGroup itemTypeAvailableGroup = new HorizontalGroup();
		itemTypeAvailableGroup.space(managementSkin.getFont("default-font-24").getSpaceXadvance());
		itemTypeAvailableGroup.addActor(new Label(translate(i18nKey), managementSkin, "table_value_label"));
		itemTypeAvailableGroup.addActor(new Label(String.valueOf(value), managementSkin, "table_value_label"));
		return itemTypeAvailableGroup;
	}

	private Drawable randomBtnResourceItemBg() {
		return btnResourceItemVariants[random.nextInt(btnResourceItemVariants.length)];
	}

	private String translate(String key) {
		return i18nTranslator.getTranslatedString(key).toString();
	}

	private Button buildIconSortButton(String drawableName) {
		Image icon = new Image(managementSkin.getDrawable(drawableName));
		ImageTextButton button = new ImageTextButton("", managementSkin, "sort_by_button");
		button.defaults().padRight(9f);
		button.add(icon);
		Image image = button.getImage(); //Swap actors or cells doesn't work, absolute agony
		button.removeActor(image);
		button.add(image);
		return button;
	}

	private Button buildTextSortButton(String i18nKey) {
		ImageTextButton button = new ImageTextButton(translate(i18nKey), managementSkin, "sort_by_button");
		button.defaults().padRight(9f);
		Image image = button.getImage(); //Swap actors or cells doesn't work, absolute agony
		button.removeActor(image);
		button.add(image);
		return button;
	}
	
	//TODO: do we want this still, some way to jump to the real world item
//			clickableRow.setAction(() -> {
//				Entity target = itemEntity;
//				while (target.getLocationComponent().getContainerEntity() != null) {
//					target = target.getLocationComponent().getContainerEntity();
//				}
//				Vector2 position = target.getLocationComponent().getWorldOrParentPosition();
//
//				if (position != null) {
//					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
//					messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, position);
//					messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, new Selectable(target, 0));
//				} else {
//					Logger.error("Attempting to move to entity with no position or container");
//				}
//			});


}
