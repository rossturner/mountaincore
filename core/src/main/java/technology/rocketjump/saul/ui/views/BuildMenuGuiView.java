package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static technology.rocketjump.saul.ui.GameInteractionMode.REMOVE_DESIGNATIONS;

@Singleton
public class BuildMenuGuiView implements GuiView, DisplaysText, Telegraph {

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
	private Table furnitureTable;
	private final Table cancelDeconstructButtons = new Table();
	private boolean displayed;

	private BuildMenuSelection currentSelection;
	private Map<BuildMenuSelection, Image> selectionImageButtons = new HashMap<>();

	@Inject
	public BuildMenuGuiView(MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository,
							I18nTranslator i18nTranslator, GameInteractionStateContainer interactionStateContainer,
							FurnitureMaterialsWidget furnitureMaterialsWidget,
							GameMaterialDictionary materialDictionary, FloorTypeDictionary floorTypeDictionary) {
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

		furnitureTable = new Table();
		furnitureTable.defaults().padRight(5);

		headerContainer = new Table();
		headerContainer.setBackground(skin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));

		messageDispatcher.addListener(this, MessageType.INTERACTION_MODE_CHANGED);
	}

	@Override
	public void onShow() {
		this.displayed = true;
		this.currentSelection = null;
//		FloorType floorType = applicableFloorTypes().findFirst().orElseThrow();
//		interactionStateContainer.setFloorTypeToPlace(floorType);
//		messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
//				floorType.getMaterialType(), GameMaterial.NULL_MATERIAL, floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType()));
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

		String headerText = i18nTranslator.getTranslatedString("GUI.BUILD_LABEL").toString();
		Label headerLabel = new Label(headerText, skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(headerLabel).center();

		buildCancelDeconstructButtons();
		Container<Table> cancelDeconstructButtonsContainer = new Container<>();
		cancelDeconstructButtonsContainer.setActor(cancelDeconstructButtons);
		cancelDeconstructButtonsContainer.right();
		
		Table topRow = new Table();
		topRow.add(new Container<>()).left().width(400).expandX();
		topRow.add(headerContainer).center().width(800).expandY();
		topRow.add(cancelDeconstructButtonsContainer).right().expandX().width(400);

		mainTable.defaults().padBottom(20);
		mainTable.add(topRow).top().expandX().fillX().row();

		furnitureTable.clearChildren();

		selectionImageButtons.clear();
		for (BuildMenuSelection buildMenuSelection : BuildMenuSelection.values()) {
			furnitureTable.add(buildFakeFurnitureButton(buildMenuSelection));
		}
		// TODO probably do lantern (and other furniture differently)

		mainTable.add(furnitureTable).center().row();

//		furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureType(interactionStateContainer.getFloorTypeToPlace()));
		furnitureMaterialsWidget.onMaterialSelection(material -> {
			if (material == null) {
				material = GameMaterial.NULL_MATERIAL;
			}

			switch (currentSelection) {
				case FLOORING -> {
					FloorType floorType = interactionStateContainer.getFloorTypeToPlace();
					messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
							floorType.getMaterialType(), material, floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType()));
					if (material.equals(GameMaterial.NULL_MATERIAL)) {
						material = materialDictionary.getExampleMaterial(floorType.getMaterialType());
					}
					selectionImageButtons.get(BuildMenuSelection.FLOORING).setColor(material.getColor());
				}
				default -> Logger.error("Not yet implemented: Switching material type of " + currentSelection);
			}

//			rebuildUI();
		});
		furnitureMaterialsWidget.onMaterialTypeSelection(materialType -> {
			switch (currentSelection) {
				case FLOORING -> {
					FloorType floorType = applicableFloorTypes().filter(ft -> ft.getMaterialType().equals(materialType)).findFirst().orElseThrow();
					messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
							materialType, GameMaterial.NULL_MATERIAL, floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType()));

					Image imageButton = selectionImageButtons.get(BuildMenuSelection.FLOORING);
					imageButton.setDrawable(skin.getDrawable(floorType.getSelectionDrawableName()));
					imageButton.setColor(materialDictionary.getExampleMaterial(materialType).getColor());
				}
				default -> Logger.error("Not yet implemented: Switching material type of " + currentSelection);
			}
			rebuildUI();
		});

		if (currentSelection != null) {
			mainTable.add(furnitureMaterialsWidget).center().expandX().row();
		}
	}

	private Actor buildFakeFurnitureButton(BuildMenuSelection buildSelection) {
		Container<Image> buttonContainer = new Container<>();
		if (buildSelection.equals(currentSelection)) {
			buttonContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		buttonContainer.pad(18);

		Image fakeFurnitureButton = buildSelectionImageButton(buildSelection);
		if (fakeFurnitureButton == null) {
			Logger.error("Not yet implemented: " + buildSelection);
			return new Container<>();
		}
		selectionImageButtons.put(buildSelection, fakeFurnitureButton);
		buttonContainer.size(183, 183);
		fakeFurnitureButton.addListener(new ChangeCursorOnHover(fakeFurnitureButton, GameCursor.SELECT, messageDispatcher));

		buttonContainer.setActor(fakeFurnitureButton);
		return buttonContainer;
	}

	private Image buildSelectionImageButton(BuildMenuSelection buildSelection) {
		Image image = null;
		switch (buildSelection) {
			case FLOORING -> {
				MaterialSelectionMessage floorMaterialSelection = interactionStateContainer.getFloorMaterialSelection();

				FloorType floorType = applicableFloorTypes().filter(ft -> ft.getMaterialType().equals(floorMaterialSelection.selectedMaterialType))
						.findFirst().orElseThrow();

				image = new Image(skin.getDrawable(floorType.getSelectionDrawableName()));
				tooltipFactory.simpleTooltip(image, floorType.getI18nKey(), TooltipLocationHint.ABOVE);

				GameMaterial material = floorMaterialSelection.selectedMaterial;
				if (material.equals(GameMaterial.NULL_MATERIAL)) {
					material = materialDictionary.getExampleMaterial(floorMaterialSelection.selectedMaterialType);
				}
				image.setColor(material.getColor());

				image.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						BuildMenuGuiView.this.currentSelection = buildSelection;
						FurnitureType fakeFurnitureType = new FurnitureType();
						fakeFurnitureType.setName(floorType.getFloorTypeName());
						fakeFurnitureType.setI18nKey(floorType.getI18nKey());
						fakeFurnitureType.setRequirements(new HashMap<>());
						applicableFloorTypes().forEach(floorType -> {
							fakeFurnitureType.getRequirements().putAll(floorType.getRequirements());
						});
						furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureType);
						// need to set initial materialType to be what is currently showing
						furnitureMaterialsWidget.setSelectedMaterialType(floorType.getMaterialType());

						messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
								floorType.getMaterialType(), GameMaterial.NULL_MATERIAL, floorType.getRequirements().values().iterator().next().get(0).getItemType()
						));
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FLOORING);
					}
				});
			}
		}
//		new EntityDrawable(
//				furnitureMap.getByFurnitureType(furnitureType), entityRenderer, true, messageDispatcher
//		).withBackground(background)
		return image;
	}

	private Stream<FloorType> applicableFloorTypes() {
		return floorTypeDictionary.getAllDefinitions()
				.stream().filter(FloorType::isConstructed)
				.sorted(Comparator.comparing(t -> i18nTranslator.getTranslatedString(t.getI18nKey()).toString()));
	}

	private void addFlooringButton(FloorType floorType) {
		String drawableName = floorType.getSelectionDrawableName() != null ? floorType.getSelectionDrawableName() : "placeholder";
		Image flooringButton = new Image(skin.getDrawable(drawableName));

		GameMaterial floorMaterial = materialDictionary.getExampleMaterial(floorType.getMaterialType());
		if (floorType.equals(interactionStateContainer.getFloorTypeToPlace()) && !interactionStateContainer.getFloorMaterialSelection().selectedMaterial.equals(GameMaterial.NULL_MATERIAL)) {
			floorMaterial = interactionStateContainer.getFloorMaterialSelection().selectedMaterial;
		}
		flooringButton.setColor(floorMaterial.getColor());
		flooringButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
						floorType.getMaterialType(), GameMaterial.NULL_MATERIAL, floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType()));
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

		furnitureTable.add(container);
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
		return GuiViewName.BUILD_MENU;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.CONSTRUCTION_MENU;
	}

	private FurnitureType fakeFurnitureType(FloorType floorType) {
//		MaterialSelectionMessage materialSelection = interactionStateContainer.getFloorMaterialSelection();
		FurnitureType fakeFurnitureType = new FurnitureType();
		fakeFurnitureType.setName(floorType.getFloorTypeName());
		fakeFurnitureType.setRequirements(floorType.getRequirements());

		return fakeFurnitureType;
	}

	private void buildCancelDeconstructButtons() {
		cancelDeconstructButtons.clearChildren();

		Container<Button> deconstructContainer = new Container<>();
		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DECONSTRUCT)) {
			deconstructContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		deconstructContainer.pad(18);
		Button deconstructButton = new Button(skin.getDrawable("btn_demolish_small"));
		deconstructButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DECONSTRUCT);
			}
		});
		deconstructButton.addListener(new ChangeCursorOnHover(deconstructContainer, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(deconstructButton, "GUI.DECONSTRUCT_LABEL", TooltipLocationHint.ABOVE);
		deconstructContainer.setActor(deconstructButton);
		cancelDeconstructButtons.add(deconstructContainer);

		Container<Button> cancelContainer = new Container<>();
		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_DESIGNATIONS)) {
			cancelContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		cancelContainer.pad(18);
		Button cancelButton = new Button(skin.getDrawable("btn_cancel_small"));
		cancelButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, REMOVE_DESIGNATIONS);
			}
		});
		cancelButton.addListener(new ChangeCursorOnHover(cancelContainer, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(cancelButton, "GUI.CANCEL_LABEL", TooltipLocationHint.ABOVE);
		cancelContainer.setActor(cancelButton);
		cancelDeconstructButtons.add(cancelContainer).padRight(20);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.INTERACTION_MODE_CHANGED -> rebuildUI();
			default -> Logger.error(String.format("Unrecognised message type received by %s, message type: %d", getClass().getSimpleName(), msg.message));
		}
		return false;
	}

	public enum BuildMenuSelection {

		FLOORING,
		WALLS,
		DOOR,
		BRIDGE,
		PILLAR

	}

}
