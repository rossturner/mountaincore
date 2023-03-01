package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;
import technology.rocketjump.saul.ui.widgets.furniture.FurnitureRequirementsWidget;
import technology.rocketjump.saul.ui.widgets.text.DecoratedString;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringFactory;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabel;
import technology.rocketjump.saul.ui.widgets.text.DecoratedStringLabelFactory;

import java.util.List;

import static technology.rocketjump.saul.ui.GameInteractionMode.CANCEL;
import static technology.rocketjump.saul.ui.views.RoomEditingView.FURNITURE_PER_ROW;

@Singleton
public class PowerWaterMenuGuiView implements GuiView, DisplaysText, Telegraph {

	private final Button backButton;
	private final Table mainTable;
	private final Table headerContainer;
	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer interactionStateContainer;
	private final FurnitureRequirementsWidget furnitureRequirementsWidget;
	private final GameMaterialDictionary materialDictionary;
	private final List<FurnitureType> furnitureTypes;
	private Table furnitureTable;
	private final Table cancelDeconstructButtons = new Table();
	private final DecoratedStringFactory decoratedStringFactory;
	private final DecoratedStringLabelFactory decoratedStringLabelFactory;
	private final EntityRenderer entityRenderer;
	private final RoomEditorFurnitureMap furnitureMap;
	private final ButtonFactory buttonFactory;
	private boolean displayed;

	@Inject
	public PowerWaterMenuGuiView(MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, GuiSkinRepository skinRepository,
								 I18nTranslator i18nTranslator, GameInteractionStateContainer interactionStateContainer,
								 FurnitureRequirementsWidget furnitureRequirementsWidget,
								 GameMaterialDictionary materialDictionary, DecoratedStringFactory decoratedStringFactory,
								 DecoratedStringLabelFactory decoratedStringLabelFactory,
								 FurnitureTypeDictionary furnitureTypeDictionary, EntityRenderer entityRenderer, RoomEditorFurnitureMap furnitureMap, ButtonFactory buttonFactory) {
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		skin = skinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureRequirementsWidget = furnitureRequirementsWidget;
		this.materialDictionary = materialDictionary;
		this.decoratedStringFactory = decoratedStringFactory;
		this.decoratedStringLabelFactory = decoratedStringLabelFactory;
		this.entityRenderer = entityRenderer;
		this.furnitureMap = furnitureMap;
		this.buttonFactory = buttonFactory;

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
		furnitureTypes = furnitureTypeDictionary.getForGuiView(getName());

		messageDispatcher.addListener(this, MessageType.INTERACTION_MODE_CHANGED);
	}

	@Override
	public void onShow() {
		this.displayed = true;
		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		if (!this.displayed) {
			return;
		}
		backButton.clearListeners();
		buttonFactory.attachClickCursor(backButton, GameCursor.SELECT);
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
			}
		});
		tooltipFactory.simpleTooltip(backButton, "GUI.BACK_LABEL", TooltipLocationHint.ABOVE);

		mainTable.clearChildren();
		headerContainer.clearChildren();

		String headerText = i18nTranslator.getTranslatedString("GUI.POWER_LABEL").toString();
		Label headerLabel = new Label(headerText, skin.get("title-header", Label.LabelStyle.class));
		headerLabel.setAlignment(Align.center);

		Container<Button> infoContainer = new Container<>();
		infoContainer.pad(18);
		infoContainer.align(Align.left);
		Button infoButton = new Button(skin.get("btn_info", Button.ButtonStyle.class));

		DecoratedString infoString = decoratedStringFactory.translate("GUI.POWER_WATER_INFO");
		DecoratedStringLabel infoContents = decoratedStringLabelFactory.create(infoString, "tooltip-text", skin);
		tooltipFactory.complexTooltip(infoButton, infoContents, TooltipFactory.TooltipBackground.LARGE_PATCH_LIGHT);

		infoContainer.setActor(infoButton);
		cancelDeconstructButtons.add(infoContainer);

		headerContainer.add(new Container<>()).width(150);
		headerContainer.add(headerLabel).width(500);
		headerContainer.add(infoContainer).width(150);

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

		furnitureTable.add(buildInteractionModeButton(GameInteractionMode.DESIGNATE_DIG_CHANNEL,
				"btn_power_and_water_irrigation", "GUI.ORDERS.DIG_CHANNELS",
			() -> {
				interactionStateContainer.setFurnitureTypeToPlace(null);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_DIG_CHANNEL);
			})
		);
		furnitureTable.add(buildInteractionModeButton(GameInteractionMode.DESIGNATE_PIPING,
				"btn_power_and_water_pipes", "GUI.BUILD.PIPING",
				() -> {
					interactionStateContainer.setFurnitureTypeToPlace(null);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.PIPING);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_PIPING);
				})
		);
		furnitureTable.add(buildInteractionModeButton(GameInteractionMode.DESIGNATE_POWER_LINES,
				"btn_power_and_water_power", "GUI.BUILD.POWER",
				() -> {
					interactionStateContainer.setFurnitureTypeToPlace(null);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.MECHANISMS);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_POWER_LINES);
				})
		);

		int furnitureTableCursor = 3;
		for (FurnitureType furnitureType : furnitureTypes) {
			furnitureTable.add(buildFurnitureButton(furnitureType));
			furnitureTableCursor++;
			if (furnitureTableCursor % FURNITURE_PER_ROW == 0) {
				furnitureTable.row();
			}
		}

		mainTable.add(furnitureTable).center().row();

//		furnitureMaterialsWidget.changeSelectedFurniture(fakeFurnitureType(interactionStateContainer.getFloorTypeToPlace()));
		furnitureRequirementsWidget.onMaterialSelection(material -> {
			if (material == null) {
				material = GameMaterial.NULL_MATERIAL;
			}
			if (interactionStateContainer.getFurnitureTypeToPlace() != null) {
				Entity furnitureEntity = furnitureMap.getByFurnitureType(interactionStateContainer.getFurnitureTypeToPlace());
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
				attributes.setMaterial(material);
				messageDispatcher.dispatchMessage(MessageType.FURNITURE_MATERIAL_SELECTED);
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
			}
		});
		furnitureRequirementsWidget.onMaterialTypeSelection(materialType -> {
			if (interactionStateContainer.getFurnitureTypeToPlace() != null) {
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

		if (interactionStateContainer.getFurnitureTypeToPlace() != null) {
			mainTable.add(furnitureRequirementsWidget).center().expandX().row();
		}
	}

	private Actor buildInteractionModeButton(GameInteractionMode interactionMode, String drawableName, String i18nKey, Runnable onClick) {
		Container<Button> buttonContainer = new Container<>();
		if (interactionMode.equals(interactionStateContainer.getInteractionMode())) {
			buttonContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		buttonContainer.pad(18);

		Button button = buttonFactory.buildDrawableButton(drawableName, i18nKey, onClick);

		buttonContainer.setActor(button);
		return buttonContainer;
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
		buttonFactory.attachClickCursor(furnitureButton, GameCursor.SELECT);
		furnitureButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				PowerWaterMenuGuiView.this.interactionStateContainer.setFurnitureTypeToPlace(furnitureType);
				furnitureRequirementsWidget.changeSelectedFurniture(furnitureType);
				GameInteractionMode.PLACE_FURNITURE.setFurnitureType(interactionStateContainer.getFurnitureTypeToPlace());
				messageDispatcher.dispatchMessage(MessageType.GUI_FURNITURE_TYPE_SELECTED, interactionStateContainer.getFurnitureTypeToPlace());
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_FURNITURE);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
			}
		});
		tooltipFactory.simpleTooltip(furnitureButton, furnitureType.getI18nKey(), TooltipLocationHint.ABOVE);

		buttonContainer.setActor(furnitureButton);
		return buttonContainer;
	}

	@Override
	public void onHide() {
		this.displayed = false;
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
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
		return GuiViewName.POWER_WATER_MENU;
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
		Button deconstructButton = buttonFactory.buildDrawableButton("btn_demolish_small", "GUI.DECONSTRUCT_LABEL", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DECONSTRUCT);
		});
		deconstructContainer.setActor(deconstructButton);
		cancelDeconstructButtons.add(deconstructContainer);

		Container<Button> cancelContainer = new Container<>();
		if (interactionStateContainer.getInteractionMode().equals(CANCEL)) {
			cancelContainer.setBackground(skin.getDrawable("asset_selection_bg_cropped"));
		}
		cancelContainer.pad(18);
		Button cancelButton = buttonFactory.buildDrawableButton("btn_cancel_small", "GUI.CANCEL_LABEL", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, CANCEL);
		});
		cancelContainer.setActor(cancelButton);
		cancelDeconstructButtons.add(cancelContainer).padRight(20);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.INTERACTION_MODE_CHANGED -> rebuildUI();
			default ->
					Logger.error(String.format("Unrecognised message type received by %s, message type: %d", getClass().getSimpleName(), msg.message));
		}
		return false;
	}

}
