package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.components.creature.ProfessionsComponent;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ChangeProfessionMessage;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.ImageButton;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to select from the professions a settler can change to.
 * Just allowing all professions for now
 */
@Singleton
public class ChangeProfessionGuiView implements GuiView {

	private final int ITEMS_PER_ROW = 5;
	private final ProfessionDictionary professionDictionary;
	private final Skin uiSkin;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;

	private Table viewTable;
	private Table professionTable;
	private ScrollPane scrollPane;

	private final Label headingLabel;
	private final TextButton backButton;


	@Inject
	public ChangeProfessionGuiView(ProfessionDictionary professionDictionary, I18nWidgetFactory i18nWidgetFactory,
								   GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								   GameInteractionStateContainer gameInteractionStateContainer, I18nTranslator i18nTranslator) {
		this.professionDictionary = professionDictionary;
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;

		this.uiSkin = guiSkinRepository.getDefault();
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.i18nTranslator = i18nTranslator;

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");

		headingLabel = i18nWidgetFactory.createLabel("GUI.CHANGE_PROFESSION_LABEL");
		viewTable.add(headingLabel).center();
		viewTable.row();

		professionTable = new Table(uiSkin);

		scrollPane = new ScrollPane(professionTable, uiSkin);
		scrollPane.setScrollingDisabled(true, false);
		scrollPane.setForceScroll(false, true);
		ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle(uiSkin.get(ScrollPane.ScrollPaneStyle.class));
		scrollPaneStyle.background = null;
		scrollPane.setStyle(scrollPaneStyle);
		scrollPane.setFadeScrollBars(false);

		viewTable.add(scrollPane);//.height(400);
		viewTable.row();

		backButton = i18nWidgetFactory.createTextButton("GUI.BACK_LABEL");
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
			}
		});
		viewTable.add(backButton).pad(10).left();
	}

	@Override
	public void populate(Table containerTable) {
		professionTable.clear();

		int numAdded = 0;

		List<Profession> professionsForSelection = new ArrayList<>(professionDictionary.getAll());
		ProfessionsComponent professionsComponent = gameInteractionStateContainer.getSelectable().getEntity().getComponent(ProfessionsComponent.class);
		for (ProfessionsComponent.QuantifiedProfession quantifiedProfession : professionsComponent.getActiveProfessions()) {
			professionsForSelection.remove(quantifiedProfession.getProfession());
		}

		for (Profession profession : professionsForSelection) {
			Table innerTable = new Table(uiSkin);

			ImageButton imageButton = profession.getImageButton();
			imageButton.setAction(() -> {
				messageDispatcher.dispatchMessage(MessageType.CHANGE_PROFESSION, new ChangeProfessionMessage(
						gameInteractionStateContainer.getSelectable().getEntity(), gameInteractionStateContainer.getProfessionToReplace(), profession
				));
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
			});
			innerTable.add(imageButton).pad(10).row();
			String skillLevelText = i18nTranslator.getSkillLevelDescription(professionsComponent.getSkillLevel(profession)).toString();
			skillLevelText += " (" + professionsComponent.getSkillLevel(profession) + ")";
			innerTable.add(new Label(skillLevelText, uiSkin)).row();
			innerTable.add(i18nWidgetFactory.createLabel(profession.getI18nKey()));

			professionTable.add(innerTable).pad(3);
			numAdded++;

			if (numAdded % ITEMS_PER_ROW == 0) {
				professionTable.row();
			}
		}

		containerTable.add(viewTable);
	}

	@Override
	public void update() {

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.CHANGE_PROFESSION;
	}

	@Override
	public GuiViewName getParentViewName() {
		// General cancel (right-click) de-selects current entity, so can't go back to entity selected view, just go back to default menu
		return GuiViewName.DEFAULT_MENU;
	}
}
