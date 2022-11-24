package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.WallTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.MaterialSelectionMessage;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.sprites.BridgeTypeDictionary;
import technology.rocketjump.saul.sprites.model.BridgeType;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;
import technology.rocketjump.saul.ui.widgets.FurnitureMaterialsWidget;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static technology.rocketjump.saul.ui.GameInteractionMode.REMOVE_DESIGNATIONS;
import static technology.rocketjump.saul.ui.views.RoomEditingView.FURNITURE_PER_ROW;

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
	private final FurnitureType doorFurnitureType;
	private final List<FurnitureType> placeAnywhereFurniture;
	private Table furnitureTable;
	private final Table cancelDeconstructButtons = new Table();
	private final WallTypeDictionary wallTypeDictionary;
	private final BridgeTypeDictionary bridgeTypeDictionary;
	private final EntityRenderer entityRenderer;
	private final RoomEditorFurnitureMap furnitureMap;
	private boolean displayed;

	private BuildMenuSelection currentSelection;
	private Map<BuildMenuSelection, Image> selectionImageButtons = new HashMap<>();

	@Inject
	public BuildMenuGuiView(MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository,
							I18nTranslator i18nTranslator, GameInteractionStateContainer interactionStateContainer,
							FurnitureMaterialsWidget furnitureMaterialsWidget,
							GameMaterialDictionary materialDictionary, FloorTypeDictionary floorTypeDictionary,
							WallTypeDictionary wallTypeDictionary, BridgeTypeDictionary bridgeTypeDictionary,
							FurnitureTypeDictionary furnitureTypeDictionary, EntityRenderer entityRenderer, RoomEditorFurnitureMap furnitureMap) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		skin = skinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureMaterialsWidget = furnitureMaterialsWidget;
		this.materialDictionary = materialDictionary;
		this.floorTypeDictionary = floorTypeDictionary;
		this.wallTypeDictionary = wallTypeDictionary;
		this.bridgeTypeDictionary = bridgeTypeDictionary;
		this.entityRenderer = entityRenderer;
		this.furnitureMap = furnitureMap;

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

		// MODDING move the selection of door furniture types to be based on an "IS_DOOR" tag
		doorFurnitureType = furnitureTypeDictionary.getByName("SINGLE_DOOR");
		placeAnywhereFurniture = furnitureTypeDictionary.getPlaceAnywhereFurniture();

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
		int furnitureTableCursor = 0;
		for (BuildMenuSelection buildMenuSelection : BuildMenuSelection.values()) {
			furnitureTable.add(buildFakeFurnitureButton(buildMenuSelection));
			furnitureTableCursor++;
		}
		for (FurnitureType furnitureType : placeAnywhereFurniture) {
			furnitureTable.add(buildFurnitureButton(furnitureType));
			furnitureTableCursor++;
			if (furnitureTableCursor % FURNITURE_PER_ROW == 0) {
				furnitureTable.row();
			}
		}

		mainTable.add(furnitureTable).center().row();

//		furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureType(interactionStateContainer.getFloorTypeToPlace()));
		furnitureMaterialsWidget.onMaterialSelection(material -> {
			if (material == null) {
				material = GameMaterial.NULL_MATERIAL;
			}

			if (currentSelection != null) {
				switch (currentSelection) {
					case DOOR -> {
						Entity furnitureEntity = furnitureMap.getByFurnitureType(doorFurnitureType);
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
						if (!material.equals(GameMaterial.NULL_MATERIAL)) {
							attributes.setMaterial(material);
							messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
						}
						messageDispatcher.dispatchMessage(MessageType.DOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
								attributes.getPrimaryMaterialType(), material, doorFurnitureType.getRequirements().get(attributes.getPrimaryMaterialType()).get(0).getItemType()
						));

						if (material.equals(GameMaterial.NULL_MATERIAL)) {
							attributes.setMaterial(materialDictionary.getExampleMaterial(attributes.getPrimaryMaterialType()));
						}
					}
					case FLOORING -> {
						FloorType floorType = interactionStateContainer.getFloorTypeToPlace();
						messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
								floorType.getMaterialType(), material, floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType()));
						if (material.equals(GameMaterial.NULL_MATERIAL)) {
							material = materialDictionary.getExampleMaterial(floorType.getMaterialType());
						}
						selectionImageButtons.get(currentSelection).setColor(material.getColor());
					}
					case WALLS -> {
						WallType wallType = interactionStateContainer.getWallTypeToPlace();
						messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, new MaterialSelectionMessage(
								wallType.getMaterialType(), material, wallType.getRequirements().get(wallType.getMaterialType()).get(0).getItemType()));
						if (material.equals(GameMaterial.NULL_MATERIAL)) {
							material = materialDictionary.getExampleMaterial(wallType.getMaterialType());
						}
						selectionImageButtons.get(currentSelection).setColor(material.getColor());
					}
					case BRIDGE -> {
						BridgeType bridgeType = interactionStateContainer.getBridgeTypeToPlace();
						messageDispatcher.dispatchMessage(MessageType.BRIDGE_MATERIAL_SELECTED, new MaterialSelectionMessage(
								bridgeType.getMaterialType(), material, bridgeType.getBuildingRequirement().getItemType()));
						if (material.equals(GameMaterial.NULL_MATERIAL)) {
							material = materialDictionary.getExampleMaterial(bridgeType.getMaterialType());
						}
						selectionImageButtons.get(currentSelection).setColor(material.getColor());
					}
					default -> Logger.error("Not yet implemented: Switching material type of " + currentSelection);
				}
			} else if (interactionStateContainer.getFurnitureTypeToPlace() != null) {
				if (material != null) {
					Entity furnitureEntity = furnitureMap.getByFurnitureType(interactionStateContainer.getFurnitureTypeToPlace());
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
					attributes.setMaterial(material);
					messageDispatcher.dispatchMessage(MessageType.FURNITURE_MATERIAL_SELECTED);
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
				}
			}
		});
		furnitureMaterialsWidget.onMaterialTypeSelection(materialType -> {
			if (currentSelection != null) {
				switch (currentSelection) {
					case DOOR -> {
						Entity furnitureEntity = furnitureMap.getByFurnitureType(doorFurnitureType);
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
						attributes.setPrimaryMaterialType(materialType);
						if (!attributes.getMaterials().containsKey(materialType)) {
							attributes.setMaterial(materialDictionary.getExampleMaterial(materialType));
						}
						messageDispatcher.dispatchMessage(MessageType.DOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
								materialType, GameMaterial.NULL_MATERIAL, doorFurnitureType.getRequirements().get(materialType).get(0).getItemType()
						));
						messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
					}
					case FLOORING -> {
						FloorType floorType = applicableFloorTypes().filter(ft -> ft.getMaterialType().equals(materialType)).findFirst().orElseThrow();
						messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
								materialType, GameMaterial.NULL_MATERIAL, floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType()));

						Image imageButton = selectionImageButtons.get(currentSelection);
						imageButton.setDrawable(skin.getDrawable(floorType.getSelectionDrawableName()));
						imageButton.setColor(materialDictionary.getExampleMaterial(materialType).getColor());
					}
					case WALLS -> {
						WallType wallType = applicableWallTypes().filter(ft -> ft.getMaterialType().equals(materialType)).findFirst().orElseThrow();
						messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, new MaterialSelectionMessage(
								materialType, GameMaterial.NULL_MATERIAL, wallType.getRequirements().get(wallType.getMaterialType()).get(0).getItemType()));

						Image imageButton = selectionImageButtons.get(currentSelection);
						imageButton.setDrawable(skin.getDrawable(wallType.getSelectionDrawableName()));
						imageButton.setColor(materialDictionary.getExampleMaterial(materialType).getColor());
					}
					case BRIDGE -> {
						BridgeType bridgeType = applicableBridgeTypes().filter(ft -> ft.getMaterialType().equals(materialType)).findFirst().orElseThrow();
						messageDispatcher.dispatchMessage(MessageType.BRIDGE_MATERIAL_SELECTED, new MaterialSelectionMessage(
								materialType, GameMaterial.NULL_MATERIAL, bridgeType.getBuildingRequirement().getItemType()));

						Image imageButton = selectionImageButtons.get(currentSelection);
						imageButton.setDrawable(skin.getDrawable(bridgeType.getSelectionDrawableName()));
						imageButton.setColor(materialDictionary.getExampleMaterial(materialType).getColor());
					}
					default -> Logger.error("Not yet implemented: Switching material type of " + currentSelection);
				}
			} else if (interactionStateContainer.getFurnitureTypeToPlace() != null) {
				Entity furnitureEntity = furnitureMap.getByFurnitureType(interactionStateContainer.getFurnitureTypeToPlace());
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
				attributes.setPrimaryMaterialType(materialType);
				if (!attributes.getMaterials().containsKey(materialType)) {
					attributes.setMaterial(materialDictionary.getExampleMaterial(materialType));
				}
				messageDispatcher.dispatchMessage(MessageType.FURNITURE_MATERIAL_SELECTED);
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
			}
			rebuildUI();
		});

		if (currentSelection != null || interactionStateContainer.getFurnitureTypeToPlace() != null) {
			mainTable.add(furnitureMaterialsWidget).center().expandX().row();
		}
	}

	private Actor buildFurnitureButton(FurnitureType furnitureType) {
		Container<Button> buttonContainer = new Container<>();
		if (furnitureType.equals(interactionStateContainer.getFurnitureTypeToPlace())) {
			buttonContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		buttonContainer.pad(18);

		Drawable background = skin.getDrawable("asset_bg");
		Button furnitureButton = new Button(new EntityDrawable(
				furnitureMap.getByFurnitureType(furnitureType), entityRenderer, true, messageDispatcher
		).withBackground(background));
		buttonContainer.size(background.getMinWidth(), background.getMinHeight());
		furnitureButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				setSelectedFurniture(furnitureType);
				furnitureMaterialsWidget.changeSelectedFurniture(furnitureType);
				GameInteractionMode.PLACE_FURNITURE.setFurnitureType(interactionStateContainer.getFurnitureTypeToPlace());
				messageDispatcher.dispatchMessage(MessageType.GUI_FURNITURE_TYPE_SELECTED, interactionStateContainer.getFurnitureTypeToPlace());
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FURNITURE);
			}
		});
		furnitureButton.addListener(new ChangeCursorOnHover(furnitureButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(furnitureButton, furnitureType.getI18nKey(), TooltipLocationHint.ABOVE);

		buttonContainer.setActor(furnitureButton);
		return buttonContainer;
	}

	private Actor buildFakeFurnitureButton(BuildMenuSelection buildSelection) {
		Container<Actor> buttonContainer = new Container<>();
		if (buildSelection.equals(currentSelection)) {
			buttonContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		buttonContainer.pad(18);

		Actor fakeFurnitureButton = buildSelectionButton(buildSelection);
		if (fakeFurnitureButton == null) {
			Logger.error("Not yet implemented: " + buildSelection);
			return new Container<>();
		}
		if (fakeFurnitureButton instanceof Image imageButton) {
			selectionImageButtons.put(buildSelection, imageButton);
		}
		buttonContainer.size(183, 183);
		fakeFurnitureButton.addListener(new ChangeCursorOnHover(fakeFurnitureButton, GameCursor.SELECT, messageDispatcher));

		buttonContainer.setActor(fakeFurnitureButton);
		return buttonContainer;
	}

	private Actor buildSelectionButton(BuildMenuSelection buildSelection) {
		switch (buildSelection) {
			case DOOR -> {
				Drawable background = skin.getDrawable("asset_bg");
				Entity doorEntity = furnitureMap.getByFurnitureType(doorFurnitureType);
				EntityDrawable entityDrawable = new EntityDrawable(
						doorEntity, entityRenderer, true, messageDispatcher
				).withBackground(background);
				entityDrawable.setScreenPositionOffset(0, 64);
				Button furnitureButton = new Button(entityDrawable);
				furnitureButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) doorEntity.getPhysicalEntityComponent().getAttributes();
						MaterialSelectionMessage newMaterialSelection = new MaterialSelectionMessage(
								attributes.getPrimaryMaterialType(),
								GameMaterial.NULL_MATERIAL, // need to go back to null material for furniture requirements to match up
								doorFurnitureType.getRequirements().get(attributes.getPrimaryMaterialType()).get(0).getItemType()
						);
						setCurrentSelection(buildSelection);
						furnitureMaterialsWidget.changeSelectedFurniture(doorFurnitureType);
						messageDispatcher.dispatchMessage(MessageType.DOOR_MATERIAL_SELECTED, newMaterialSelection);
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_DOOR);
					}
				});
				tooltipFactory.simpleTooltip(furnitureButton, doorFurnitureType.getI18nKey(), TooltipLocationHint.ABOVE);
				return furnitureButton;
			}
			case FLOORING -> {
				MaterialSelectionMessage materialSelection = interactionStateContainer.getFloorMaterialSelection();

				FloorType floorType = applicableFloorTypes().filter(ft -> ft.getMaterialType().equals(materialSelection.selectedMaterialType))
						.findFirst().orElseThrow();

				Image image = new Image(skin.getDrawable(floorType.getSelectionDrawableName()));
				final Image imageRef = image;
				tooltipFactory.simpleTooltip(image, floorType.getI18nKey(), TooltipLocationHint.ABOVE);

				GameMaterial material = materialSelection.selectedMaterial;
				if (material.equals(GameMaterial.NULL_MATERIAL)) {
					material = materialDictionary.getExampleMaterial(materialSelection.selectedMaterialType);
				}
				image.setColor(material.getColor());

				image.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						MaterialSelectionMessage newMaterialSelection = new MaterialSelectionMessage(
								interactionStateContainer.getFloorMaterialSelection().selectedMaterialType,
								GameMaterial.NULL_MATERIAL, // need to go back to null material for furniture requirements to match up
								interactionStateContainer.getFloorMaterialSelection().resourceItemType
						);
						FloorType floorType = applicableFloorTypes().filter(ft -> ft.getMaterialType().equals(newMaterialSelection.selectedMaterialType))
								.findFirst().orElseThrow();

						setCurrentSelection(buildSelection);
						FurnitureType fakeFurnitureType = new FurnitureType();
						fakeFurnitureType.setName(floorType.getFloorTypeName());
						fakeFurnitureType.setI18nKey(floorType.getI18nKey());
						fakeFurnitureType.setRequirements(new HashMap<>());
						applicableFloorTypes().forEach(applicableType -> {
							fakeFurnitureType.getRequirements().putAll(applicableType.getRequirements());
						});
						furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureType);
						// need to set initial materialType to be what is currently showing
						furnitureMaterialsWidget.setSelectedMaterialType(floorType.getMaterialType());
						imageRef.setColor(actualOrExampleColor(newMaterialSelection.selectedMaterial, newMaterialSelection.selectedMaterialType));

						messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, newMaterialSelection);
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FLOORING);
					}
				});
				return wrapWithBackground(image);
			}
			case WALLS -> {
				MaterialSelectionMessage materialSelection = interactionStateContainer.getWallMaterialSelection();

				WallType wallType = applicableWallTypes().filter(ft -> ft.getMaterialType().equals(materialSelection.selectedMaterialType))
						.findFirst().orElseThrow();

				Image image = new Image(skin.getDrawable(wallType.getSelectionDrawableName()));
				final Image imageRef = image;
				tooltipFactory.simpleTooltip(image, wallType.getI18nKey(), TooltipLocationHint.ABOVE);

				GameMaterial material = materialSelection.selectedMaterial;
				if (material.equals(GameMaterial.NULL_MATERIAL)) {
					material = materialDictionary.getExampleMaterial(materialSelection.selectedMaterialType);
				}
				image.setColor(material.getColor());

				image.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						MaterialSelectionMessage newMaterialSelection = new MaterialSelectionMessage(
								interactionStateContainer.getWallMaterialSelection().selectedMaterialType,
								GameMaterial.NULL_MATERIAL, // need to go back to null material for furniture requirements to match up
								interactionStateContainer.getWallMaterialSelection().resourceItemType
						);
						WallType wallType = applicableWallTypes().filter(ft -> ft.getMaterialType().equals(newMaterialSelection.selectedMaterialType))
								.findFirst().orElseThrow();

						setCurrentSelection(buildSelection);
						FurnitureType fakeFurnitureType = new FurnitureType();
						fakeFurnitureType.setName(wallType.getWallTypeName());
						fakeFurnitureType.setI18nKey(wallType.getI18nKey());
						fakeFurnitureType.setRequirements(new HashMap<>());
						applicableWallTypes().forEach(applicableType -> {
							fakeFurnitureType.getRequirements().putAll(applicableType.getRequirements());
						});
						furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureType);
						// need to set initial materialType to be what is currently showing
						furnitureMaterialsWidget.setSelectedMaterialType(wallType.getMaterialType());
						imageRef.setColor(actualOrExampleColor(newMaterialSelection.selectedMaterial, newMaterialSelection.selectedMaterialType));

						messageDispatcher.dispatchMessage(MessageType.WALL_MATERIAL_SELECTED, newMaterialSelection);
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_WALLS);
					}
				});

				return wrapWithBackground(image);
			}
			case BRIDGE -> {
				MaterialSelectionMessage materialSelection = interactionStateContainer.getBridgeMaterialSelection();
				BridgeType bridgeType = applicableBridgeTypes().filter(type -> type.getMaterialType().equals(materialSelection.selectedMaterialType))
						.findFirst().orElseThrow();

				Image image = new Image(skin.getDrawable(bridgeType.getSelectionDrawableName()));
				final Image imageRef = image;
				tooltipFactory.simpleTooltip(image, bridgeType.getI18nKey(), TooltipLocationHint.ABOVE);

				GameMaterial material = materialSelection.selectedMaterial;
				if (material.equals(GameMaterial.NULL_MATERIAL)) {
					material = materialDictionary.getExampleMaterial(materialSelection.selectedMaterialType);
				}
				image.setColor(material.getColor());

				image.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						MaterialSelectionMessage newMaterialSelection = new MaterialSelectionMessage(
								interactionStateContainer.getBridgeMaterialSelection().selectedMaterialType,
								GameMaterial.NULL_MATERIAL, // need to go back to null material for furniture requirements to match up
								interactionStateContainer.getBridgeMaterialSelection().resourceItemType
						);
						BridgeType bridgeType = applicableBridgeTypes().filter(type -> type.getMaterialType().equals(newMaterialSelection.selectedMaterialType))
								.findFirst().orElseThrow();

						setCurrentSelection(buildSelection);
						FurnitureType fakeFurnitureType = new FurnitureType();
						fakeFurnitureType.setName(bridgeType.getMaterialType().name()+"_BRIDGE");
						fakeFurnitureType.setI18nKey(bridgeType.getI18nKey());
						fakeFurnitureType.setRequirements(new HashMap<>());
						applicableWallTypes().forEach(wallType -> {
							fakeFurnitureType.getRequirements().putAll(wallType.getRequirements());
						});
						furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureType);
						// need to set initial materialType to be what is currently showing
						furnitureMaterialsWidget.setSelectedMaterialType(bridgeType.getMaterialType());
						imageRef.setColor(actualOrExampleColor(newMaterialSelection.selectedMaterial, newMaterialSelection.selectedMaterialType));

						messageDispatcher.dispatchMessage(MessageType.BRIDGE_MATERIAL_SELECTED, newMaterialSelection);
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_BRIDGE);
					}
				});

				return wrapWithBackground(image);
			}
			default -> {
				return null;
			}
		}
	}

	private void setCurrentSelection(BuildMenuSelection buildSelection) {
		this.currentSelection = buildSelection;
		this.interactionStateContainer.setFurnitureTypeToPlace(null);
	}

	private void setSelectedFurniture(FurnitureType furnitureType) {
		this.currentSelection = null;
		this.interactionStateContainer.setFurnitureTypeToPlace(furnitureType);
	}

	private Actor wrapWithBackground(Image image) {
		Container<Actor> imageContainer = new Container<>();
		imageContainer.setBackground(skin.getDrawable("asset_bg"));
		imageContainer.setActor(image);
		return imageContainer;
	}

	private Color actualOrExampleColor(GameMaterial material, GameMaterialType materialType) {
		if (material.equals(GameMaterial.NULL_MATERIAL)) {
			return materialDictionary.getExampleMaterial(materialType).getColor();
		} else {
			return material.getColor();
		}
	}

	private Stream<FloorType> applicableFloorTypes() {
		return floorTypeDictionary.getAllDefinitions()
				.stream().filter(FloorType::isConstructed)
				.sorted(Comparator.comparing(t -> i18nTranslator.getTranslatedString(t.getI18nKey()).toString()));
	}

	private Stream<WallType> applicableWallTypes() {
		return wallTypeDictionary.getAllDefinitions()
				.stream().filter(WallType::isConstructed)
				.sorted(Comparator.comparing(t -> i18nTranslator.getTranslatedString(t.getI18nKey()).toString()));
	}

	private Stream<BridgeType> applicableBridgeTypes() {
		return bridgeTypeDictionary.getAll()
				.stream()
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
		this.interactionStateContainer.setFurnitureTypeToPlace(null);
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
		BRIDGE

	}

}
