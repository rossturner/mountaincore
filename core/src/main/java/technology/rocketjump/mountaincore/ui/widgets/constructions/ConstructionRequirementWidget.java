package technology.rocketjump.mountaincore.ui.widgets.constructions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.settlement.ItemAvailabilityChecker;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWordClass;
import technology.rocketjump.mountaincore.ui.widgets.EntityDrawable;
import technology.rocketjump.mountaincore.ui.widgets.crafting.CraftingHintWidgetFactory;
import technology.rocketjump.mountaincore.ui.widgets.text.DecoratedString;
import technology.rocketjump.mountaincore.ui.widgets.text.DecoratedStringFactory;
import technology.rocketjump.mountaincore.ui.widgets.text.DecoratedStringLabel;
import technology.rocketjump.mountaincore.ui.widgets.text.DecoratedStringLabelFactory;

import java.util.Map;

public class ConstructionRequirementWidget extends Table {

	private final QuantifiedItemTypeWithMaterial requirement;
	private final Construction construction;
	private final MessageDispatcher messageDispatcher;
	private final ItemAvailabilityChecker itemAvailabilityChecker;
	private final I18nTranslator i18nTranslator;

	private final Entity itemEntity;
	private final Skin skin;
	private final Image entityImage;
	private final Stack entityStack;
	private final Table tooltipTable;
	private final EntityDrawable entityDrawable;
	private final GameContext gameContext;
	private final CraftingHintWidgetFactory craftingHintWidgetFactory;
	private final DecoratedStringFactory decoratedStringFactory;
	private final DecoratedStringLabelFactory decoratedStringLabelFactory;

	private Label label;

	public ConstructionRequirementWidget(QuantifiedItemTypeWithMaterial requirement, Construction construction, Entity itemEntity,
	                                     Skin skin, MessageDispatcher messageDispatcher,
	                                     ItemAvailabilityChecker itemAvailabilityChecker, I18nTranslator i18nTranslator, EntityRenderer entityRenderer,
	                                     TooltipFactory tooltipFactory, GameContext gameContext, CraftingHintWidgetFactory craftingHintWidgetFactory,
	                                     DecoratedStringFactory decoratedStringFactory, DecoratedStringLabelFactory decoratedStringLabelFactory) {
		this.requirement = requirement;
		this.construction = construction;
		this.skin = skin;
		this.messageDispatcher = messageDispatcher;
		this.itemAvailabilityChecker = itemAvailabilityChecker;
		this.i18nTranslator = i18nTranslator;
		this.itemEntity = itemEntity;
		this.gameContext = gameContext;
		this.craftingHintWidgetFactory = craftingHintWidgetFactory;
		this.decoratedStringFactory = decoratedStringFactory;
		this.decoratedStringLabelFactory = decoratedStringLabelFactory;

		this.defaults().pad(4);

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
		tooltipTable.defaults().padBottom(30);
		rebuildTooltipTable();

		tooltipFactory.complexTooltip(entityStack, tooltipTable, TooltipFactory.TooltipBackground.LARGE_PATCH_DARK);
		entityStack.add(entityImage);
		entityStack.add(amountTable);

		label = new Label("", skin.get("default-red", Label.LabelStyle.class));
		updateLabel();

		this.add(entityStack).size(120, 120).center().row();
		this.add(label).expandX().center().row();
	}

	public void update() {
		updateEntity();
		updateLabel();
		rebuildTooltipTable();
	}

	private void rebuildTooltipTable() {
		tooltipTable.clearChildren();

		String headerText = i18nTranslator.getTranslatedString(requirement.getItemType().getI18nKey()).toString();
		tooltipTable.add(new Label(headerText, skin.get("complex-tooltip-header", Label.LabelStyle.class))).center().row();

		String itemDescriptionText = i18nTranslator.getTranslatedString(requirement.getItemType().getI18nKey(), I18nWordClass.TOOLTIP).toString();
		Label descriptionLabel = new Label(itemDescriptionText, skin);
		descriptionLabel.setWrap(true);
		tooltipTable.add(descriptionLabel).width(700).center().row();


		for (String hint : craftingHintWidgetFactory.getCraftingRecipeDescriptions(requirement.getItemType(), requirement.getMaterial())) {
			Label requirementLabel = new Label(hint, skin);
			requirementLabel.setWrap(true);
			tooltipTable.add(requirementLabel).width(700).center().row();
		}

		DecoratedString availabilityText = decoratedStringFactory.translate("CONSTRUCTION.REQUIREMENT_ALLOCATION", Map.of(
				"numRequired", DecoratedString.fromString(String.valueOf(requirement.getQuantity())),
				"numAssigned", DecoratedString.fromString(String.valueOf(construction.getAllocationAmount(requirement.getItemType(), gameContext))),
				"numAvailable", DecoratedString.fromString(String.valueOf(itemAvailabilityChecker.getAmountAvailable(requirement.getItemType(), requirement.getMaterial())))
		));
		DecoratedStringLabel decoratedStringLabel = decoratedStringLabelFactory.create(availabilityText, "default", skin);
		tooltipTable.add(decoratedStringLabel).center().row();
	}


	private void updateEntity() {
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		attributes.setQuantity(Math.min(requirement.getQuantity(), attributes.getItemType().getMaxStackSize()));
		if (requirement.getMaterial() != null && !GameMaterial.NULL_MATERIAL.equals(requirement.getMaterial())) {
			attributes.setMaterial(requirement.getMaterial());
		}

		int availableAmount = itemAvailabilityChecker.getAmountAvailable(requirement.getItemType(), requirement.getMaterial()) +
				construction.getAllocationAmount(requirement.getItemType(), gameContext);
		if (availableAmount >= requirement.getQuantity()) {
			entityDrawable.setOverrideColor(null);
		} else {
			entityDrawable.setOverrideColor(HexColors.GHOST_NEGATIVE_COLOR);
		}

		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemEntity);
	}

	private void updateLabel() {
		if (requirement.getMaterial() == null || GameMaterial.NULL_MATERIAL.equals(requirement.getMaterial())) {
			label.setText(i18nTranslator.getTranslatedString("MATERIAL_TYPE.ANY").toString());
		} else {
			label.setText(i18nTranslator.getTranslatedString(requirement.getMaterial().getI18nKey()).toString());
		}
	}

}
