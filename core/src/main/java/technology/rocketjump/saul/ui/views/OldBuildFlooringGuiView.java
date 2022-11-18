package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.MaterialSelectionMessage;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.actions.SetInteractionMode;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonStyle;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.IconButton;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

import java.util.*;

import static technology.rocketjump.saul.materials.model.GameMaterial.NULL_MATERIAL;

@Singleton
public class OldBuildFlooringGuiView implements GuiView, DisplaysText {

	private final SettlementItemTracker settlementItemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;


	private Table viewTable;
	private final SelectBox<GameMaterialType> materialTypeSelect;
	private final SelectBox<String> materialSelect;
	// I18nUpdatable
	private final Label headingLabel;
	private final Label typeLabel;
	private final Label materialLabel;

	private final Map<GameMaterialType, ItemType> resourceTypeMap = new TreeMap<>();
	private final Map<String, GameMaterial> currentMaterialNamesMap = new HashMap<>();
	private final List<IconButton> iconButtons = new LinkedList<>();

	private GameMaterialType selectedMaterialType;
	private GameMaterial selectedMaterial;
	boolean initialised;

	@Inject
	public OldBuildFlooringGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								   IconButtonFactory iconButtonFactory, FloorTypeDictionary floorTypeDictionary, SettlementItemTracker settlementItemTracker,
								   I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory) {
		this.messageDispatcher = messageDispatcher;
		this.settlementItemTracker = settlementItemTracker;
		this.i18nTranslator = i18nTranslator;

		for (FloorType floorType : floorTypeDictionary.getAllDefinitions()) {
			if (floorType.isConstructed()) {
				List<QuantifiedItemType> requirements = floorType.getRequirements().get(floorType.getMaterialType());
				if (requirements.size() == 1 && requirements.get(0).getQuantity() == 1) {
					resourceTypeMap.put(floorType.getMaterialType(), requirements.get(0).getItemType());
				} else {
					Logger.error(floorType.getFloorTypeName() + " must only have a single requirements ingredient");
				}
			}
		}

		Skin uiSkin = guiSkinRepository.getDefault();

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");
//		viewTable.setDebug(true);


		headingLabel = i18NWidgetFactory.createLabel("GUI.BUILD.FLOOR");
		viewTable.add(headingLabel).center().colspan(2);
		viewTable.row();

		Table materialTable = new Table(uiSkin);

		this.typeLabel = i18NWidgetFactory.createLabel("MATERIAL_TYPE");
		materialTable.add(typeLabel);
		materialTable.row();


		materialTypeSelect = new SelectBox<>(uiSkin);
		materialTypeSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				selectedMaterialType = materialTypeSelect.getSelected();
				resetMaterialSelect();
			}
		});
		materialTable.add(materialTypeSelect).pad(5);
		materialTable.row();

		this.materialLabel = i18NWidgetFactory.createLabel("MATERIAL");
		materialTable.add(materialLabel);
		materialTable.row();


		materialSelect = new SelectBox<>(uiSkin);
		Array<String> materialNamesArray = new Array<>();
		materialNamesArray.add("SOMETHING");
		materialSelect.setItems(materialNamesArray);
		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				onMaterialSelectionChange();
			}
		});

		materialTable.add(materialSelect).pad(5);
		materialTable.row();


		viewTable.add(materialTable);

		Table iconTable = new Table(uiSkin);

		IconButton floorIconButton = iconButtonFactory.create("GUI.BUILD.FLOOR", "floorboards", HexColors.get("#F1F1E0"), ButtonStyle.DEFAULT);
		floorIconButton.setAction(new SetInteractionMode(GameInteractionMode.PLACE_FLOORING, messageDispatcher));
		iconButtons.add(floorIconButton);

		for (IconButton iconButton : iconButtons) {
			iconTable.add(iconButton).pad(5);
		}

		viewTable.add(iconTable);

		viewTable.row();

		resetMaterialTypeSelect();
		rebuildUI();
		initialised = true;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.OLD_BUILD_FLOORING;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.OLD_BUILD_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();

		resetMaterialSelect();

		containerTable.add(viewTable);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	@Override
	public void rebuildUI() {
		resetMaterialTypeSelect(); // For different material names
	}

	private void resetMaterialTypeSelect() {
		Array<GameMaterialType> itemsArray = new Array<>();
		for (GameMaterialType gameMaterialType : resourceTypeMap.keySet()) {
			itemsArray.add(gameMaterialType);
		}
		materialTypeSelect.setItems(itemsArray);
		materialTypeSelect.setSelected(itemsArray.get(0));
		resetMaterialSelect();
	}

	private void resetMaterialSelect() {
		currentMaterialNamesMap.clear();
		Array<String> materialTypes = new Array<>();

		String anyString = i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString();
		currentMaterialNamesMap.put(anyString, NULL_MATERIAL);
		materialTypes.add(anyString);

		ItemType itemTypeForMaterialType = resourceTypeMap.get(selectedMaterialType);

		if (itemTypeForMaterialType != null) {
			Set<GameMaterial> materialsByItemType = settlementItemTracker.getMaterialsByItemType(itemTypeForMaterialType);
			if (materialsByItemType != null) {
				for (GameMaterial gameMaterial : materialsByItemType) {
					materialTypes.add(gameMaterial.getMaterialName());
					currentMaterialNamesMap.put(gameMaterial.getMaterialName(), gameMaterial);
				}
			}
		}

		materialSelect.setItems(materialTypes);
		if (materialTypes.size > 0) {
			materialSelect.setSelected(materialTypes.get(0));
			selectedMaterial = currentMaterialNamesMap.get(materialTypes.get(0));
		}
		onMaterialSelectionChange();
	}

	private void onMaterialSelectionChange() {
		selectedMaterial = currentMaterialNamesMap.get(materialSelect.getSelected());
		if (initialised) {
			messageDispatcher.dispatchMessage(MessageType.FLOOR_MATERIAL_SELECTED, new MaterialSelectionMessage(
					selectedMaterialType, selectedMaterial, resourceTypeMap.get(selectedMaterialType)));
		}
	}
}
