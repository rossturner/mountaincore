package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.WallTypeDictionary;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.MaterialSelectionMessage;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.FurnitureMaterialsWidget;

import java.util.Comparator;
import java.util.stream.Stream;

@Singleton
public class BuildWallsGuiView implements GuiView, DisplaysText {

	private final Button backButton;
	private final Table mainTable;
	private final Table headerContainer;
	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer interactionStateContainer;
	private final FurnitureMaterialsWidget furnitureMaterialsWidget;
	private final GameMaterialDictionary materialDictionary;
	private final WallTypeDictionary wallTypeDictionary;
	private Table wallsTable;
	private boolean displayed;

	@Inject
	public BuildWallsGuiView(MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository,
							 I18nTranslator i18nTranslator, GameInteractionStateContainer interactionStateContainer,
							 FurnitureMaterialsWidget furnitureMaterialsWidget,
							 GameMaterialDictionary materialDictionary, WallTypeDictionary wallTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		skin = skinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureMaterialsWidget = furnitureMaterialsWidget;
		this.materialDictionary = materialDictionary;
		this.wallTypeDictionary = wallTypeDictionary;

		backButton = new Button(skin.getDrawable("btn_back"));
		mainTable = new Table();
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.getDrawable("asset_dwarf_select_bg"));
		mainTable.pad(20);
		mainTable.top();

		wallsTable = new Table();

		headerContainer = new Table();
		headerContainer.setBackground(skin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));
	}

	@Override
	public void onShow() {
		this.displayed = true;
		WallType wallType = applicableWallTypes().findFirst().orElseThrow();
		messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, new MaterialSelectionMessage(
				wallType.getMaterialType(), GameMaterial.NULL_MATERIAL, wallType.getRequirements().get(wallType.getMaterialType()).get(0).getItemType()));
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_WALLS);
		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		if (!this.displayed) {
			return;
		}
		backButton.clearListeners();
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
			}
		});
		backButton.addListener(new ChangeCursorOnHover(backButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(backButton, "GUI.BACK_LABEL", TooltipLocationHint.ABOVE);

		mainTable.clearChildren();
		headerContainer.clearChildren();

		String headerText = headerText = i18nTranslator.getTranslatedString("GUI.BUILD.WALLS").toString();
		Label headerLabel = new Label(headerText, skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(headerLabel).center();

		Table topRow = new Table();
		topRow.add(new Container<>()).left().expandX();
		topRow.add(headerContainer).center().width(800).expandY();
		topRow.add(new Container<>()).right().expandX();

		mainTable.defaults().padBottom(20);
		mainTable.add(topRow).top().expandX().fillX().row();

		wallsTable.clearChildren();
		applicableWallTypes().forEach(this::addWallButton);
		mainTable.add(wallsTable).center().row();

		furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureTypeForWall());
		furnitureMaterialsWidget.onMaterialSelection(material -> {
			if (material == null) {
				material = GameMaterial.NULL_MATERIAL;
			}
			WallType wallType = interactionStateContainer.getWallTypeToPlace();
			messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, new MaterialSelectionMessage(
					wallType.getMaterialType(), material, wallType.getRequirements().get(wallType.getMaterialType()).get(0).getItemType()));
			rebuildUI();
		});
		furnitureMaterialsWidget.onMaterialTypeSelection(materialType -> {
			Logger.error("Not yet implemented: Switching material type of walls");
		});

		mainTable.add(furnitureMaterialsWidget).center().expandX().row();
	}

	private Stream<WallType> applicableWallTypes() {
		return wallTypeDictionary.getAllDefinitions()
				.stream().filter(WallType::isConstructed)
				.sorted(Comparator.comparing(t -> i18nTranslator.getTranslatedString(t.getI18nKey()).toString()));
	}

	private void addWallButton(WallType wallType) {
		String drawableName = wallType.getSelectionDrawableName() != null ? wallType.getSelectionDrawableName() : "placeholder";
		Image wallButton = new Image(skin.getDrawable(drawableName));

		GameMaterial wallMaterial = materialDictionary.getExampleMaterial(wallType.getMaterialType());
		if (wallType.equals(interactionStateContainer.getWallTypeToPlace()) && !interactionStateContainer.getWallMaterialSelection().selectedMaterial.equals(GameMaterial.NULL_MATERIAL)) {
			wallMaterial = interactionStateContainer.getWallMaterialSelection().selectedMaterial;
		}
		wallButton.setColor(wallMaterial.getColor());
		wallButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, new MaterialSelectionMessage(
						wallType.getMaterialType(), GameMaterial.NULL_MATERIAL, wallType.getRequirements().get(wallType.getMaterialType()).get(0).getItemType()));
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_WALLS);
				rebuildUI();
			}
		});
		wallButton.addListener(new ChangeCursorOnHover(wallButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(wallButton, wallType.getI18nKey(), TooltipLocationHint.ABOVE);

		Container<Image> container = new Container<>();
		container.pad(18);
		container.setActor(wallButton);
		if (wallType.equals(interactionStateContainer.getWallTypeToPlace())) {
			container.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_WALLS);
		}

		wallsTable.add(container);
	}

	@Override
	public void onHide() {
		this.displayed = false;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();
		containerTable.add(backButton).left().bottom().padLeft(30).padRight(50);
		containerTable.add(mainTable);
	}

	@Override
	public void update() {

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_WALLS;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.BUILD_MENU;
	}

	private FurnitureType fakeFurnitureTypeForWall() {
		WallType wallType = interactionStateContainer.getWallTypeToPlace();
		FurnitureType fakeFurnitureType = new FurnitureType();
		fakeFurnitureType.setName(wallType.getWallTypeName());
		fakeFurnitureType.setRequirements(wallType.getRequirements());
		return fakeFurnitureType;
	}
}
