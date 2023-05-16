package technology.rocketjump.mountaincore.ui.widgets.furniture;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;
import technology.rocketjump.mountaincore.settlement.ItemAvailabilityChecker;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.I18nString;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWord;
import technology.rocketjump.mountaincore.ui.i18n.I18nWordClass;
import technology.rocketjump.mountaincore.ui.skins.MenuSkin;
import technology.rocketjump.mountaincore.ui.widgets.EntityDrawable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FurnitureRequirementWidget extends Table {

	private final QuantifiedItemType requirement;
	private final MessageDispatcher messageDispatcher;
	private final ItemAvailabilityChecker itemAvailabilityChecker;
	private final I18nTranslator i18nTranslator;

	private final Array<GameMaterial> materials;
	private final Entity itemEntity;
	private final GameMaterial defaultDisplayMaterial;
	private final Skin skin;
	private final Image entityImage;
	private final Stack entityStack;
	private final Table tooltipTable;
	private final SelectBox<GameMaterial> materialSelect;
	private final EntityDrawable entityDrawable;
	private Consumer<GameMaterial> callback;

	public FurnitureRequirementWidget(QuantifiedItemType requirement, Entity itemEntity, Skin skin, MessageDispatcher messageDispatcher,
									  ItemAvailabilityChecker itemAvailabilityChecker, I18nTranslator i18nTranslator, EntityRenderer entityRenderer,
									  TooltipFactory tooltipFactory, GameMaterial defaultDisplayMaterial, SoundAssetDictionary soundAssetDictionary, MenuSkin menuSkin) {
		this.requirement = requirement;
		this.skin = skin;
		this.messageDispatcher = messageDispatcher;
		this.itemAvailabilityChecker = itemAvailabilityChecker;
		this.i18nTranslator = i18nTranslator;
		this.itemEntity = itemEntity;
		this.defaultDisplayMaterial = defaultDisplayMaterial;

		this.defaults().pad(4);

		materials = new Array<>();
		materials.add(GameMaterial.NULL_MATERIAL);
		itemAvailabilityChecker.getAvailableMaterialsFor(requirement.getItemType(), requirement.getQuantity()).forEach(materials::add);
		materialSelect = new SelectBox<>(menuSkin, "select_narrow_alt") {
			@Override
			protected String toString(GameMaterial item) {
				if (item == GameMaterial.NULL_MATERIAL) {
					return i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString();
				} else {
					return i18nTranslator.getTranslatedString(item.getI18nKey()).toString();
				}
			}
		};
		materialSelect.setAlignment(Align.center);
		materialSelect.getList().setAlignment(Align.center);

		materialSelect.setItems(materials);

		materialSelect.getSelection().setProgrammaticChangeEvents(false);
		materialSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				changeSelection(getSelectedMaterial());
			}
		});

		materialSelect.addListener(new ChangeCursorOnHover(materialSelect, GameCursor.SELECT, messageDispatcher));
		materialSelect.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary, "VeryLightHover", "ConfirmVeryLight"));
		if (materials.size == 1) {
			materialSelect.setDisabled(true);
			materialSelect.getColor().a = 0.5f;
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

		rebuildUI();
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
		GameMaterial selectedMaterial = getSelectedMaterial();
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
		this.add(entityStack).size(120, 120).row();
		this.add(materialSelect).growX().center().row();
	}

	private void changeSelection(GameMaterial gameMaterial) {
		if (callback != null) {
			if (gameMaterial == GameMaterial.NULL_MATERIAL) {
				callback.accept(null);
			} else {
				callback.accept(gameMaterial);
			}
		}
		updateEntity();
		rebuildUI();
		rebuildTooltipTable();
	}

	private void updateEntity() {
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		attributes.setQuantity(Math.min(requirement.getQuantity(), attributes.getItemType().getMaxStackSize()));

		GameMaterial selected = getSelectedMaterial();
		if (selected == null) {
			selected = defaultDisplayMaterial;
		}
		attributes.setMaterial(selected);

		boolean isAvailable = itemAvailabilityChecker.getAmountAvailable(requirement.getItemType(), selected) >= requirement.getQuantity();
		if (isAvailable) {
			entityDrawable.setOverrideColor(null);
		} else {
			entityDrawable.setOverrideColor(HexColors.GHOST_NEGATIVE_COLOR);
		}

		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemEntity);
	}

	private GameMaterial getSelectedMaterial() {
		GameMaterial selected = materialSelect.getSelected();
		if (selected == GameMaterial.NULL_MATERIAL) {
			return null;
		}
		return selected;
	}

	public void onMaterialSelection(Consumer<GameMaterial> callback) {
		this.callback = callback;
	}
}
