package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.tags.CraftingStationBehaviourTag;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.rooms.constructions.FurnitureConstruction;
import technology.rocketjump.mountaincore.screens.ManagementScreenName;
import technology.rocketjump.mountaincore.ui.GameInteractionStateContainer;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.constructions.ConstructionPriorityWidget;
import technology.rocketjump.mountaincore.ui.widgets.constructions.ConstructionRequirementsWidget;

@Singleton
public class ConstructionSelectedGuiView implements GuiView, DisplaysText {

	private final Skin skin;
	private final GameInteractionStateContainer interactionStateContainer;
	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;

	private final Table mainTable;
	private final Table headerContainer;
	private final I18nTranslator i18nTranslator;
	private final Table descriptionTable;
	private boolean displayed;
	private Construction selectedConstruction;
	private final ConstructionRequirementsWidget constructionRequirementsWidget;
	private final SoundAssetDictionary soundAssetDictionary;
	private final ButtonFactory buttonFactory;

	@Inject
	public ConstructionSelectedGuiView(GuiSkinRepository guiSkinRepository, GameInteractionStateContainer interactionStateContainer,
									   MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, I18nTranslator i18nTranslator,
									   ConstructionRequirementsWidget constructionRequirementsWidget, SoundAssetDictionary soundAssetDictionary,
									   ButtonFactory buttonFactory) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.interactionStateContainer = interactionStateContainer;
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		this.i18nTranslator = i18nTranslator;
		this.constructionRequirementsWidget = constructionRequirementsWidget;
		this.soundAssetDictionary = soundAssetDictionary;
		this.buttonFactory = buttonFactory;

		mainTable = new Table();
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.getDrawable("asset_dwarf_select_bg"));
		mainTable.pad(20);
		mainTable.defaults().padBottom(20);
		mainTable.top();

		headerContainer = new Table();
		headerContainer.setBackground(skin.get("asset_bg_ribbon_title_patch", TenPatchDrawable.class));

		descriptionTable = new Table();
		descriptionTable.defaults().padBottom(20).padTop(20);
	}

	@Override
	public void rebuildUI() {
		if (!this.displayed || interactionStateContainer.getSelectable() == null || interactionStateContainer.getSelectable().getConstruction() == null) {
			return;
		}
		this.selectedConstruction = interactionStateContainer.getSelectable().getConstruction();

		mainTable.clearChildren();
		headerContainer.clearChildren();

		I18nText headlineDescription = selectedConstruction.getHeadlineDescription(i18nTranslator);
		Label headerLabel = new Label(headlineDescription.toString(), skin.get("title-header", Label.LabelStyle.class));
		headerContainer.add(headerLabel).center();

		Table actionButtons = new Table();
		if (selectedConstruction instanceof FurnitureConstruction furnitureConstruction) {
			PhysicalEntityComponent physicalEntityComponent = furnitureConstruction.getFurnitureEntityToBePlaced().getPhysicalEntityComponent();
			if (physicalEntityComponent.getAttributes() instanceof FurnitureEntityAttributes attributes) {
				boolean isCraftingStation = attributes.getFurnitureType().hasTag(CraftingStationBehaviourTag.class);

				if (isCraftingStation) {
					Container<Button> craftingButtonContainer = new Container<>();
					Button craftingButton = buttonFactory.buildDrawableButton("btn_recipe", "GUI.CRAFTING_MANAGEMENT.TITLE", () -> {
						messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, ManagementScreenName.CRAFTING.name());
					});
					craftingButtonContainer.setActor(craftingButton);

					actionButtons.add(craftingButton);
				}
			}
		}

		Table topRow = new Table();
		topRow.add(new Container<>()).width(150);
		topRow.add(headerContainer).expandX();
		topRow.add(actionButtons).width(150);
		mainTable.add(topRow).padTop(20).growX().row();

		updateDescriptionTable();
		mainTable.add(descriptionTable).growX().row();

		mainTable.add(new ConstructionPriorityWidget(selectedConstruction, skin, tooltipFactory, messageDispatcher, soundAssetDictionary)).center().row();

		constructionRequirementsWidget.setSelectedConstruction(selectedConstruction);
		mainTable.add(constructionRequirementsWidget).padBottom(40).center().row();
	}

	private void updateDescriptionTable() {
		descriptionTable.clearChildren();
		selectedConstruction.getConstructionStatusDescriptions(i18nTranslator, messageDispatcher).forEach((description) -> {
			Label descriptionLabel = new Label(description.toString(), skin.get("default-red", Label.LabelStyle.class));
			descriptionTable.add(descriptionLabel).center().row();
		});
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(mainTable).padLeft(20).padBottom(10);
	}

	@Override
	public void update() {
		updateDescriptionTable();
		constructionRequirementsWidget.update();
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.CONSTRUCTION_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void onShow() {
		this.displayed = true;
		rebuildUI();
	}

	@Override
	public void onHide() {
		this.displayed = false;
	}
}
