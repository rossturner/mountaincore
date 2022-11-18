package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.MaterialSelectionMessage;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rooms.RoomFactory;
import technology.rocketjump.saul.rooms.RoomStore;
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
import technology.rocketjump.saul.ui.widgets.GameDialogDictionary;

import java.util.Comparator;
import java.util.stream.Stream;

@Singleton
public class BuildFlooringGuiView implements GuiView, DisplaysText {

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
	private final FloorTypeDictionary floorTypeDictionary;
	private Table flooringTable;
	private boolean displayed;

	@Inject
	public BuildFlooringGuiView(MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository,
								I18nTranslator i18nTranslator, GameInteractionStateContainer interactionStateContainer,
								FurnitureTypeDictionary furnitureTypeDictionary, RoomEditorFurnitureMap furnitureMap,
								EntityRenderer entityRenderer, RoomStore roomStore, RoomEditorItemMap itemMap,
								PlantSpeciesDictionary plantSpeciesDictionary, FurnitureMaterialsWidget furnitureMaterialsWidget,
								RoomFactory roomFactory, GameMaterialDictionary materialDictionary, GameDialogDictionary gameDialogDictionary,
								ItemTypeDictionary itemTypeDictionary, SoundAssetDictionary soundAssetDictionary, FloorTypeDictionary floorTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		skin = skinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureMaterialsWidget = furnitureMaterialsWidget;
		this.materialDictionary = materialDictionary;
		this.floorTypeDictionary = floorTypeDictionary;

		backButton = new Button(skin.getDrawable("btn_back"));
		mainTable = new Table();
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.getDrawable("asset_dwarf_select_bg"));
		mainTable.pad(20);
		mainTable.top();

		flooringTable = new Table();

		headerContainer = new Table();
		headerContainer.setBackground(skin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));
	}

	@Override
	public void onShow() {
		this.displayed = true;
		FloorType floorType = applicableFloorTypes().findFirst().orElseThrow();
//		interactionStateContainer.setFloorTypeToPlace(floorType);
		messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
				floorType.getMaterialType(), GameMaterial.NULL_MATERIAL, floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType()));
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

		String headerText = headerText = i18nTranslator.getTranslatedString("GUI.BUILD.FLOOR").toString();
		Label headerLabel = new Label(headerText, skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(headerLabel).center();

		Table topRow = new Table();
		topRow.add(new Container<>()).left().expandX();
		topRow.add(headerContainer).center().width(800).expandY();
		topRow.add(new Container<>()).right().expandX();

		mainTable.defaults().padBottom(20);
		mainTable.add(topRow).top().expandX().fillX().row();

		flooringTable.clearChildren();
		applicableFloorTypes().forEach(this::addFlooringButton);
		mainTable.add(flooringTable).center().row();

//		mainTable.add(furnitureMaterialsWidget).center().expandX().row();
	}

	private Stream<FloorType> applicableFloorTypes() {
		return floorTypeDictionary.getAllDefinitions()
				.stream().filter(FloorType::isConstructed)
				.sorted(Comparator.comparing(t -> i18nTranslator.getTranslatedString(t.getI18nKey()).toString()));
	}

	private void addFlooringButton(FloorType floorType) {
		String drawableName = floorType.getSelectionDrawableName() != null ? floorType.getSelectionDrawableName() : "placeholder";
		Image flooringButton = new Image(skin.getDrawable(drawableName));
		flooringButton.setColor(materialDictionary.getExampleMaterial(floorType.getMaterialType()).getColor());
		flooringButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				interactionStateContainer.setFloorTypeToPlace(floorType);
				rebuildUI();
			}
		});
		flooringButton.addListener(new ChangeCursorOnHover(flooringButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(flooringButton, floorType.getI18nKey(), TooltipLocationHint.ABOVE);

		Container<Image> container = new Container<>();
		container.pad(18);
		container.setActor(flooringButton);
		if (floorType.equals(interactionStateContainer.getFloorTypeToPlace())) {
			container.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FLOORING);
		}

		flooringTable.add(container);
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
		return GuiViewName.BUILD_FLOORING;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.BUILD_MENU;
	}
}
