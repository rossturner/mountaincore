package technology.rocketjump.saul.ui.widgets.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.ItemAvailabilityChecker;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.views.RoomEditorFurnitureMap;
import technology.rocketjump.saul.ui.views.RoomEditorItemMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class FurnitureRequirementsWidget extends Table implements DisplaysText {

	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final ItemAvailabilityChecker itemAvailabilityChecker;
	private final I18nTranslator i18nTranslator;
	private final RoomEditorFurnitureMap roomEditorFurnitureMap;
	private final RoomEditorItemMap roomEditorItemMap;
	private final EntityRenderer entityRenderer;
	private final TooltipFactory tooltipFactory;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final SoundAssetDictionary soundAssetDictionary;

	private FurnitureType selectedFurnitureType;
	private GameMaterialType selectedMaterialType;
	private List<ItemTypeWithMaterial> materialSelections = new ArrayList<>();

	private final Table materialTypeSelection = new Table();
	private final Table itemMaterialSelection = new Table();
	private Consumer<GameMaterial> materialSelectionMade;
	private Consumer<GameMaterialType> materialTypeSelectionMade;

	@Inject
	public FurnitureRequirementsWidget(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
									   ItemAvailabilityChecker itemAvailabilityChecker, I18nTranslator i18nTranslator,
									   RoomEditorFurnitureMap roomEditorFurnitureMap, RoomEditorItemMap roomEditorItemMap,
									   EntityRenderer entityRenderer, TooltipFactory tooltipFactory, GameMaterialDictionary gameMaterialDictionary,
									   SoundAssetDictionary soundAssetDictionary) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.itemAvailabilityChecker = itemAvailabilityChecker;
		this.i18nTranslator = i18nTranslator;
		this.roomEditorFurnitureMap = roomEditorFurnitureMap;
		this.roomEditorItemMap = roomEditorItemMap;
		this.entityRenderer = entityRenderer;
		this.tooltipFactory = tooltipFactory;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
	}

	public void changeSelectedFurniture(FurnitureType furnitureType) {
		if (furnitureType == null) {
			selectedFurnitureType = null;
			return;
		}

		if (!furnitureType.equals(selectedFurnitureType)) {
			this.selectedFurnitureType = furnitureType;
			Entity furnitureEntity = roomEditorFurnitureMap.getByFurnitureType(furnitureType);
			if (furnitureEntity != null) {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
				this.selectedMaterialType = attributes.getPrimaryMaterialType();
			} else {
				this.selectedMaterialType = furnitureType.getRequirements().keySet().iterator().next();
			}
			this.materialSelections.clear();

			rebuildUI();
		}
	}

	@Override
	public void rebuildUI() {
		this.clearChildren();
		materialTypeSelection.clearChildren();
		itemMaterialSelection.clearChildren();
		itemMaterialSelection.defaults().padRight(20);

		if (selectedFurnitureType == null) {
			return;
		}

		ButtonGroup<Button> materialTypeGroup = new ButtonGroup<>();
		materialTypeGroup.setMaxCheckCount(1);
		for (GameMaterialType materialType : selectedFurnitureType.getRequirements().keySet()) {
			Button materialTypeButton = new Button(skin.get("btn_material_type", Button.ButtonStyle.class));
			if (materialType.equals(selectedMaterialType)) {
				materialTypeButton.setChecked(true);
			}
			materialTypeButton.add(new Label(materialType.getI18nValue().toString(), skin.get("default", Label.LabelStyle.class)));


			materialTypeButton.addListener(new ChangeCursorOnHover(materialTypeButton, GameCursor.SELECT, messageDispatcher));
			materialTypeButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
			materialTypeButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					materialTypeChanged(materialType);
				}
			});

			materialTypeGroup.add(materialTypeButton);
			materialTypeSelection.add(materialTypeButton).pad(5);
		}
		materialTypeGroup.setMinCheckCount(1);
		materialTypeGroup.setUncheckLast(true);
		this.add(materialTypeSelection).left();

		rebuildMaterialSelections();
		this.add(itemMaterialSelection).center();
	}

	private void rebuildMaterialSelections() {
		itemMaterialSelection.clearChildren();

		List<QuantifiedItemType> requirements = selectedFurnitureType.getRequirements().get(selectedMaterialType);
		for (QuantifiedItemType requirement : requirements) {
			if (requirement.isLiquid()) {
				continue;
			}

			FurnitureRequirementWidget furnitureRequirementWidget = new FurnitureRequirementWidget(requirement, roomEditorItemMap.getByItemType(requirement.getItemType()),
					skin, messageDispatcher, itemAvailabilityChecker, i18nTranslator, entityRenderer,
					tooltipFactory, gameMaterialDictionary.getExampleMaterial(requirement.getItemType().getPrimaryMaterialType()), soundAssetDictionary);
			furnitureRequirementWidget.onMaterialSelection(material -> {
				List<ItemTypeWithMaterial> otherMaterialSelections = new ArrayList<>(materialSelections.stream()
						.filter(s -> !s.getItemType().equals(requirement.getItemType()))
						.toList());

				if (material != null) {
					ItemTypeWithMaterial newSelection = new ItemTypeWithMaterial();
					newSelection.setItemType(requirement.getItemType());
					newSelection.setMaterial(material);
					otherMaterialSelections.add(newSelection);
				}
				if (this.materialSelectionMade != null) {
					this.materialSelectionMade.accept(material);
				}
				this.materialSelections = otherMaterialSelections;

				messageDispatcher.dispatchMessage(MessageType.FURNITURE_MATERIAL_SELECTED);
			});

			itemMaterialSelection.add(furnitureRequirementWidget);
		}
	}

	public void onMaterialSelection(Consumer<GameMaterial> materialSelectionMade) {
		this.materialSelectionMade = materialSelectionMade;
	}

	public void onMaterialTypeSelection(Consumer<GameMaterialType> materialTypeSelectionMade) {
		this.materialTypeSelectionMade = materialTypeSelectionMade;
	}

	private void materialTypeChanged(GameMaterialType materialType) {
		this.selectedMaterialType = materialType;
		if (this.materialTypeSelectionMade != null) {
			this.materialTypeSelectionMade.accept(materialType);
		}
		rebuildMaterialSelections();
	}

	public GameMaterialType getSelectedMaterialType() {
		return selectedMaterialType;
	}

	public void setSelectedMaterialType(GameMaterialType materialType) {
		this.selectedMaterialType = materialType;
		rebuildUI();
	}

	public void setSelectedMaterial(GameMaterial material) {
	}

	public List<ItemTypeWithMaterial> getSelections() {
		return materialSelections;
	}

	public FurnitureType getSelectedFurnitureType() {
		return selectedFurnitureType;
	}
}
