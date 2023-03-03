package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.constructions.ConstructionPriorityWidget;
import technology.rocketjump.saul.ui.widgets.constructions.ConstructionRequirementsWidget;

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

	@Inject
	public ConstructionSelectedGuiView(GuiSkinRepository guiSkinRepository, GameInteractionStateContainer interactionStateContainer,
									   MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory, I18nTranslator i18nTranslator,
									   ConstructionRequirementsWidget constructionRequirementsWidget, SoundAssetDictionary soundAssetDictionary) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.interactionStateContainer = interactionStateContainer;
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		this.i18nTranslator = i18nTranslator;
		this.constructionRequirementsWidget = constructionRequirementsWidget;
		this.soundAssetDictionary = soundAssetDictionary;

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

		Table topRow = new Table();
		topRow.add(new Container<>()).width(150);
		topRow.add(headerContainer).expandX();
		topRow.add(new Container<>()).width(150);
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
