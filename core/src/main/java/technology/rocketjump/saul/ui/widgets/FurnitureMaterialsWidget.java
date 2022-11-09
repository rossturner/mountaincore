package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.settlement.ItemAvailabilityChecker;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.views.RoomEditorFurnitureMap;
import technology.rocketjump.saul.ui.views.RoomEditorItemMap;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class FurnitureMaterialsWidget extends Table implements DisplaysText {

	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final ItemAvailabilityChecker itemAvailabilityChecker;
	private final I18nTranslator i18nTranslator;
	private final RoomEditorFurnitureMap roomEditorFurnitureMap;
	private final RoomEditorItemMap roomEditorItemMap;
	private final EntityRenderer entityRenderer;
	private final TooltipFactory tooltipFactory;

	private FurnitureType selectedFurnitureType;
	private GameMaterialType selectedMaterialType;
	private List<ItemTypeWithMaterial> materialSelections = new ArrayList<>();

	private final Table materialTypeSelection = new Table();
	private final Table itemMaterialSelection = new Table();
	private final Table availabilityTable = new Table();

	@Inject
	public FurnitureMaterialsWidget(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
									ItemAvailabilityChecker itemAvailabilityChecker, I18nTranslator i18nTranslator,
									RoomEditorFurnitureMap roomEditorFurnitureMap, RoomEditorItemMap roomEditorItemMap, EntityRenderer entityRenderer, TooltipFactory tooltipFactory) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.itemAvailabilityChecker = itemAvailabilityChecker;
		this.i18nTranslator = i18nTranslator;
		this.roomEditorFurnitureMap = roomEditorFurnitureMap;
		this.roomEditorItemMap = roomEditorItemMap;
		this.entityRenderer = entityRenderer;
		this.tooltipFactory = tooltipFactory;

	}

	public void changeSelectedFurniture(FurnitureType furnitureType) {
		if (furnitureType == null) {
			selectedFurnitureType = null;
			return;
		}

		if (!furnitureType.equals(selectedFurnitureType)) {
			this.selectedFurnitureType = furnitureType;
			this.selectedMaterialType = furnitureType.getRequirements().isEmpty() ? null : furnitureType.getRequirements().keySet().iterator().next();
			this.materialSelections.clear();

			rebuildUI();
		}
	}

	@Override
	public void rebuildUI() {
		this.clearChildren();
		materialTypeSelection.clearChildren();
		itemMaterialSelection.clearChildren();
		availabilityTable.clearChildren();

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

			materialTypeButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					materialTypeChanged(materialType);
				}
			});
			materialTypeButton.addListener(new ChangeCursorOnHover(materialTypeButton, GameCursor.SELECT, messageDispatcher));

			materialTypeGroup.add(materialTypeButton);
			materialTypeSelection.add(materialTypeButton).pad(5);
		}
		materialTypeGroup.setMinCheckCount(1);
		materialTypeGroup.setUncheckLast(true);
		this.add(materialTypeSelection).left();

		rebuildMaterialSelections();
		this.add(itemMaterialSelection).center();

		this.add(availabilityTable).right();
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
					tooltipFactory, roomEditorFurnitureMap.getExampleMaterialFor(requirement.getItemType().getPrimaryMaterialType()));
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
				this.materialSelections = otherMaterialSelections;

				// TODO virtual placing room changed?

				rebuildAvailability();
			});

			itemMaterialSelection.add(furnitureRequirementWidget);
		}

		rebuildAvailability();
	}

	private void rebuildAvailability() {
		availabilityTable.clearChildren();

		List<QuantifiedItemType> requirements = selectedFurnitureType.getRequirements().get(selectedMaterialType);
		for (QuantifiedItemType requirement : requirements) {

			GameMaterial selectedMaterial = materialSelections.stream().filter(s -> s.getItemType().equals(requirement.getItemType()))
					.map(ItemTypeWithMaterial::getMaterial)
					.findFirst().orElse(null);


			String labelText = i18nTranslator.getAvailabilityDescription(requirement.getItemType(), itemAvailabilityChecker.getAmountAvailable(requirement.getItemType(), selectedMaterial), selectedMaterial).toString();

			availabilityTable.add(new Label(labelText, skin.get("default-red", Label.LabelStyle.class))).left().row();
		}
	}

	private void materialTypeChanged(GameMaterialType materialType) {
		this.selectedMaterialType = materialType;

		rebuildMaterialSelections();
	}
}
