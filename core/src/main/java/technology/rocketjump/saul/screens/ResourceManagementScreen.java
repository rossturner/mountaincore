package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.GameDialog;

import javax.inject.Inject;
import javax.inject.Singleton;

import static technology.rocketjump.saul.rendering.camera.DisplaySettings.GUI_DESIGN_SIZE;
import static technology.rocketjump.saul.screens.ManagementScreenName.RESOURCES;

@Singleton
public class ResourceManagementScreen implements GameScreen, GameContextAware, DisplaysText {
	private final Stage stage;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final OrthographicCamera camera = new OrthographicCamera();
	private final Skin menuSkin;
	private final Skin mainGameSkin;
	private final Skin managementSkin;

//	private static final float INDENT_WIDTH = 50f;
//	private final SettlementItemTracker settlementItemTracker;
//	private final StockpileGroupDictionary stockpileGroupDictionary;
//	private final ClickableTableFactory clickableTableFactory;
//
//	private final Map<StockpileGroup, I18nLabel> groupLabels = new HashMap<>();
//	private final EntityRenderer entityRenderer;
//
//	private final Table scrollableTable;
//	private final ScrollPane scrollableTablePane;
//
//	private final Set<String> selectedRows = new HashSet<>();

	@Inject
	public ResourceManagementScreen(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
									I18nTranslator i18nTranslator, SettlementItemTracker settlementItemTracker,
									EntityRenderer entityRenderer, StockpileGroupDictionary stockpileGroupDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.stage = new Stage(new ExtendViewport(GUI_DESIGN_SIZE.x, GUI_DESIGN_SIZE.y));
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.mainGameSkin = guiSkinRepository.getMainGameSkin();
		this.managementSkin = guiSkinRepository.getManagementSkin();

//		this.settlementItemTracker = settlementItemTracker;
//		this.entityRenderer = entityRenderer;
//		this.stockpileGroupDictionary = stockpileGroupDictionary;
//
//		for (StockpileGroup group : stockpileGroupDictionary.getAll()) {
//			groupLabels.put(group, i18nWidgetFactory.createLabel(group.getI18nKey(), I18nWordClass.PLURAL));
//		}
//
//		scrollableTable = new Table(uiSkin);
//		scrollableTablePane = Scene2DUtils.wrapWithScrollPane(scrollableTable, uiSkin);
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

		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); //TODO: should this resize?

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
		rebuildUI();
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

		Table baseLayer = new Table();
		baseLayer.setBackground(menuSkin.getDrawable("paper_texture_bg"));
		baseLayer.add(new Image(menuSkin.getDrawable("paper_texture_bg_pattern_large"))).growY().padLeft(146.0f);
		baseLayer.add(buildComponentLayer()).expandX().fill();
		baseLayer.add(new Image(menuSkin.getDrawable("paper_texture_bg_pattern_large"))).growY().padRight(146.0f);


		Table baseLayerWrapper = new Table();
		baseLayerWrapper.add(baseLayer).width(2354f);
		stack.add(baseLayerWrapper);

		stage.addActor(stack);
	}

	private Table buildComponentLayer() {
		Label titleLabel = new Label(translate("GUI.RESOURCE_MANAGEMENT.TITLE"), menuSkin, "title_ribbon");
		titleLabel.setAlignment(Align.center);

		Table mainTable = new Table();
		mainTable.setBackground(managementSkin.getDrawable("accent_bg"));


		Table table = new Table();
		table.add(titleLabel).padTop(54f).row();
		table.add(mainTable).padLeft(38f).padRight(38f).spaceTop(48f).grow();
		return table;
	}

	private String translate(String key) {
		return i18nTranslator.getTranslatedString(key).toString();
	}

//	@Override
//	public void reset() {
//		containerTable.clearChildren();
//		containerTable.add(titleLabel).center().pad(5).row();
//		scrollableTable.clearChildren();
//
//		Map<StockpileGroup, Map<ItemType, Map<GameMaterial, Map<Long, Entity>>>> itemsByGroupByType = new LinkedHashMap<>();
//		Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> allByItemType = settlementItemTracker.getAllByItemType();
//		for (Map.Entry<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemTypeMapEntry : allByItemType.entrySet()) {
//			if (itemTypeMapEntry.getKey().getStockpileGroup() != null) {
//				itemsByGroupByType.computeIfAbsent(itemTypeMapEntry.getKey().getStockpileGroup(), a -> new LinkedHashMap<>())
//						.put(itemTypeMapEntry.getKey(), itemTypeMapEntry.getValue());
//			}
//		}
//
//
//		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll()) {
//			if (itemsByGroupByType.containsKey(stockpileGroup)) {
//				Table groupTable = new Table(uiSkin);
//
//				groupTable.add(groupLabels.get(stockpileGroup)).center().row();
//
//				Map<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemsByType = itemsByGroupByType.get(stockpileGroup);
//				for (Map.Entry<ItemType, Map<GameMaterial, Map<Long, Entity>>> itemTypeMapEntry : itemsByType.entrySet()) {
//					ItemType itemType = itemTypeMapEntry.getKey();
//					String itemTypeRowName = "itemType:"+itemType.getItemTypeName();
//
//					Entity firstEntity = itemTypeMapEntry.getValue().values().iterator().next().values().iterator().next();
//
//					int totalQuantity = 0;
//					int totalUnallocated = 0;
//					for (Map<Long, Entity> entityMap : itemTypeMapEntry.getValue().values()) {
//						for (Entity itemEntity : entityMap.values()) {
//							ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
//							totalQuantity += attributes.getQuantity();
//							totalUnallocated += itemEntity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
//						}
//					}
//
//					I18nText itemTypeDisplayName = i18nTranslator.getTranslatedString(itemType.getI18nKey());
//
//					addRowToTable(groupTable, firstEntity, itemTypeRowName, itemTypeDisplayName, totalUnallocated, totalQuantity, 0, false);
//
//
//					if (selectedRows.contains(itemTypeRowName)) {
//						for (Map.Entry<GameMaterial, Map<Long, Entity>> gameMaterialMapEntry : itemTypeMapEntry.getValue().entrySet()) {
//							GameMaterial material = gameMaterialMapEntry.getKey();
//							String materialRowName = itemTypeRowName + ":material:" + material.getMaterialName();
//
//							Entity firstMaterialEntity = gameMaterialMapEntry.getValue().values().iterator().next();
//
//							I18nText materialDescription = i18nTranslator.getItemDescription(1, material, itemType, null);
//
//							int totalMaterialQuantity = 0;
//							int totalMaterialUnallocated = 0;
//
//							for (Entity itemEntity : gameMaterialMapEntry.getValue().values()) {
//								ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
//								totalMaterialQuantity += attributes.getQuantity();
//								totalMaterialUnallocated += itemEntity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
//							}
//
//							addRowToTable(groupTable, firstMaterialEntity, materialRowName, materialDescription, totalMaterialUnallocated, totalMaterialQuantity, 1, false);
//
//							if (selectedRows.contains(materialRowName)) {
//								for (Entity itemEntity : gameMaterialMapEntry.getValue().values()) {
//									String entityRowName = materialRowName + ":" + itemEntity.getId();
//
//									ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
//
//									addRowToTable(groupTable, itemEntity, entityRowName, i18nTranslator.getDescription(itemEntity), itemEntity.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated(), attributes.getQuantity(), 2, true);
//								}
//
//							}
//						}
//					}
//
//				}
//				scrollableTable.add(groupTable).center().pad(5).row();
//
//			}
//		}
//
//		containerTable.add(scrollableTablePane).pad(2);
//	}
//
//	private void addRowToTable(Table groupTable, Entity itemEntity, String rowName, I18nText displayName, int unallocated, int total, int indents, boolean clickToEntity) {
//		Table rowContainerTable = new Table(uiSkin);
//		if (indents > 0) {
//			rowContainerTable.add(new Container<>()).width(indents * INDENT_WIDTH);
//		}
//
//		ClickableTable clickableRow = clickableTableFactory.create();
//		clickableRow.setBackground("default-rect");
//		clickableRow.pad(2);
//		if (clickToEntity) {
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
//		} else {
//			clickableRow.setAction(() -> {
//				if (selectedRows.contains(rowName)) {
//					selectedRows.remove(rowName);
//				} else {
//					selectedRows.add(rowName);
//				}
//				reset();
//			});
//		}
//
//		EntityDrawable materialDrawable = new EntityDrawable(itemEntity, entityRenderer, false, messageDispatcher);
//		clickableRow.add(new Image(materialDrawable)).center().width(80).pad(5);
//
//		clickableRow.add(new I18nTextWidget(displayName, uiSkin, messageDispatcher)).left().width(400f - (indents * INDENT_WIDTH)).pad(2);
//
//		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedWordWithReplacements(
//				"GUI.RESOURCE_MANAGEMENT.UNALLOCATED_LABEL", ImmutableMap.of("count", new I18nWord(String.valueOf(unallocated)))), uiSkin, messageDispatcher)
//		).center().width(100);
//		clickableRow.add(new I18nTextWidget(i18nTranslator.getTranslatedWordWithReplacements(
//				"GUI.SETTLER_MANAGEMENT.TOTAL_QUANTITY_LABEL", ImmutableMap.of("count", new I18nWord(String.valueOf(total)))), uiSkin, messageDispatcher)
//		).center().width(100);
//
//		rowContainerTable.add(clickableRow);
//		groupTable.add(rowContainerTable).right().row();
//	}


}
