package technology.rocketjump.saul.ui.widgets.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.settlement.ItemAvailabilityChecker;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.I18nString;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.i18n.I18nWordClass;
import technology.rocketjump.saul.ui.widgets.EntityDrawable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FurnitureRequirementWidget extends Table {

	private final QuantifiedItemType requirement;
	private final MessageDispatcher messageDispatcher;
	private final ItemAvailabilityChecker itemAvailabilityChecker;
	private final I18nTranslator i18nTranslator;

	private final List<GameMaterial> materials;
	private final Entity itemEntity;
	private final GameMaterial defaultDisplayMaterial;
	private final Skin skin;
	private final Button leftButton, rightButton;
	private final Image entityImage;
	private final Stack entityStack;
	private final Table tooltipTable;
	private final EntityDrawable entityDrawable;
	private int selectionIndex;

	private Label label;

	private Consumer<GameMaterial> callback;

	public FurnitureRequirementWidget(QuantifiedItemType requirement, Entity itemEntity, Skin skin, MessageDispatcher messageDispatcher,
									  ItemAvailabilityChecker itemAvailabilityChecker, I18nTranslator i18nTranslator, EntityRenderer entityRenderer,
									  TooltipFactory tooltipFactory, GameMaterial defaultDisplayMaterial, SoundAssetDictionary soundAssetDictionary) {
		this.requirement = requirement;
		this.skin = skin;
		this.messageDispatcher = messageDispatcher;
		this.itemAvailabilityChecker = itemAvailabilityChecker;
		this.i18nTranslator = i18nTranslator;
		this.itemEntity = itemEntity;
		this.defaultDisplayMaterial = defaultDisplayMaterial;

		this.defaults().pad(4);

		materials = new ArrayList<>(itemAvailabilityChecker.getAvailableMaterialsFor(requirement.getItemType(), requirement.getQuantity()));
		materials.add(0, null);


		leftButton = new Button(skin.get("btn_arrow_small_left", Button.ButtonStyle.class));
		if (materials.size() > 1) {
			leftButton.addListener(new ChangeCursorOnHover(leftButton, GameCursor.SELECT, messageDispatcher));
			leftButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary, "VeryLightHover", "ConfirmVeryLight"));
			leftButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					changeSelectionIndex(-1);
				}
			});
		} else {
			leftButton.setDisabled(true);
		}
		rightButton = new Button(skin.get("btn_arrow_small_right", Button.ButtonStyle.class));
		if (materials.size() > 1) {
			rightButton.addListener(new ChangeCursorOnHover(rightButton, GameCursor.SELECT, messageDispatcher));
			rightButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary, "VeryLightHover", "ConfirmVeryLight"));
			rightButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					changeSelectionIndex(1);
				}
			});
		} else {
			rightButton.setDisabled(true);
		}

		entityStack = new Stack();

		entityDrawable = new EntityDrawable(itemEntity, entityRenderer, true, messageDispatcher);
		entityImage = new Image(entityDrawable);
		entityImage.setFillParent(true);

		updateEntity();

		Container<Label> amountContainer = new Container<>();
		amountContainer.setBackground(skin.getDrawable("asset_bg_for_amount"));

		Label amountLabel = new Label(String.valueOf(requirement.getQuantity()), skin.get("default", Label.LabelStyle.class));
		amountContainer.setActor(amountLabel);
		amountContainer.center();
		Table amountTable = new Table();
		amountTable.add(amountContainer).left().top();
		amountTable.add(new Container<>()).expandX().row();
		amountTable.add(new Container<>()).colspan(2).expandY();

		tooltipTable = new Table();
		rebuildTooltipTable();

		tooltipFactory.complexTooltip(entityStack, tooltipTable, TooltipFactory.TooltipBackground.LARGE_PATCH_DARK);
		entityStack.add(entityImage);
		entityStack.add(amountTable);

		resetLabel();
	}

	private void rebuildTooltipTable() {
		tooltipTable.clearChildren();
		tooltipTable.defaults().padBottom(30);

		String headerText = i18nTranslator.getTranslatedString(requirement.getItemType().getI18nKey()).toString();
		tooltipTable.add(new Label(headerText, skin.get("complex-tooltip-header", Label.LabelStyle.class))).center().row();

		String itemDescriptionText = i18nTranslator.getTranslatedString(requirement.getItemType().getI18nKey(), I18nWordClass.TOOLTIP).toString();
		Label descriptionLabel = new Label(itemDescriptionText, skin);
		descriptionLabel.setWrap(true);
		tooltipTable.add(descriptionLabel).width(700).center().row();

		Map<String, I18nString> replacements = new HashMap<>();
		GameMaterial selectedMaterial = materials.get(selectionIndex);
		replacements.put("amount", new I18nWord(String.valueOf(itemAvailabilityChecker.getAmountAvailable(requirement.getItemType(), selectedMaterial))));
		if (selectedMaterial != null) {
			replacements.put("material", i18nTranslator.getTranslatedString(selectedMaterial.getI18nKey()));
		}
		I18nWord availabilityWord = i18nTranslator.getWord(selectedMaterial == null ? "GUI.FURNITURE_REQUIREMENT.ALL_MATERIALS" : "GUI.FURNITURE_REQUIREMENT.SPECIFIED_MATERIALS");
		String availabilityText = i18nTranslator.applyReplacements(availabilityWord, replacements, Gender.ANY).toString();
		tooltipTable.add(new Label(availabilityText, skin));
	}

	private void rebuildUI() {
		this.clearChildren();
		this.add(leftButton).center();
		this.add(entityStack).size(120, 120);
		this.add(rightButton).center().row();
		this.add(label).colspan(3).expandX().center().row();
	}

	private void changeSelectionIndex(int amount) {
		selectionIndex += amount;
		if (selectionIndex < 0) {
			selectionIndex = materials.size() - 1;
		}
		if (selectionIndex >= materials.size()) {
			selectionIndex = 0;
		}
		if (callback != null) {
			callback.accept(materials.get(selectionIndex));
		}
		updateEntity();
		resetLabel();
		rebuildTooltipTable();
	}

	private void updateEntity() {
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		attributes.setQuantity(Math.min(requirement.getQuantity(), attributes.getItemType().getMaxStackSize()));

		GameMaterial selected = materials.get(selectionIndex);
		if (selected == null) {
			selected = defaultDisplayMaterial;
		}
		attributes.setMaterial(selected);

		boolean isAvailable = itemAvailabilityChecker.getAmountAvailable(requirement.getItemType(), materials.get(selectionIndex)) >= requirement.getQuantity();
		if (isAvailable) {
			entityDrawable.setOverrideColor(null);
		} else {
			entityDrawable.setOverrideColor(HexColors.GHOST_NEGATIVE_COLOR);
		}

		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemEntity);
	}

	private void resetLabel() {
		GameMaterial selected = materials.get(selectionIndex);
		String text;
		if (selected == null) {
			text = i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString();
		} else {
			text = i18nTranslator.getTranslatedString(selected.getI18nKey()).toString();
		}
		label = new Label(text, skin.get("default-red", Label.LabelStyle.class));

		rebuildUI();
	}

	public void onMaterialSelection(Consumer<GameMaterial> callback) {
		this.callback = callback;
	}
}
