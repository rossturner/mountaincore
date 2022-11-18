package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.fonts.OnDemandFontRepository;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.LanguageType;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.CustomSelect;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

@Singleton
public class PrivacyOptInMenu implements Menu, DisplaysText {

	public static final int NUM_PRIVACY_OPT_IN_LINES = 3;
	private final Skin menuSkin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final WidgetFactory widgetFactory;
	private final Drawable bgDialogueBoxBackground;
	private final MenuButtonFactory menuButtonFactory;

	private Table menuTable;

	@Inject
	public PrivacyOptInMenu(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
	                        I18nTranslator i18nTranslator, OnDemandFontRepository onDemandFontRepository,
	                        WidgetFactory widgetFactory, MenuButtonFactory menuButtonFactory) {
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.menuSkin = guiSkinRepository.getMenuSkin();
		this.widgetFactory = widgetFactory;
		this.menuButtonFactory = menuButtonFactory;
		bgDialogueBoxBackground = menuSkin.getDrawable("bg_dialogue_box");


		menuTable = new Table();
		menuTable.setFillParent(false);
		menuTable.center();
		menuTable.background(bgDialogueBoxBackground);
	}

	@Override
	public void show() {

	}

	@Override
	public void hide() {

	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(menuTable).center();
	}

	@Override
	public void reset() {
		menuTable.clearChildren();
		CustomSelect<LanguageType> languageSelect = widgetFactory.createLanguageSelectBox(menuSkin);

		Table languageRow = new Table();
		languageRow.add(languageSelect).width(580f).height(80).padLeft(5);
		Label descriptionLabel = new Label(translate("PRIVACY.OPT_IN.DESCRIPTION"), menuSkin, "white_text");
		descriptionLabel.setWrap(true);

		float sidePadding = 178f;
		menuTable.add(languageRow).padTop(57f).padBottom(57f).row();
		menuTable.add(descriptionLabel).width(bgDialogueBoxBackground.getMinWidth() - 100f - sidePadding * 2).row();

		for (int dataPointIndex = 1; dataPointIndex <= NUM_PRIVACY_OPT_IN_LINES; dataPointIndex++) {
			Label dataPointLabel = new Label(translate("PRIVACY.OPT_IN.DATA_POINT."+dataPointIndex), menuSkin, "white_text");
			dataPointLabel.setWrap(true);
			menuTable.add(dataPointLabel).width(bgDialogueBoxBackground.getMinWidth() - 100f - sidePadding * 2).padTop(10f).left().row();
		}

		Container<TextButton> acceptButton = menuButtonFactory.createButton("PRIVACY.OPT_IN.ACCEPT_BUTTON", menuSkin, MenuButtonFactory.ButtonStyle.BTN_DIALOG_1)
				.withAction(() -> {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
					messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, Boolean.TRUE);
				})
				.build();

		Container<TextButton> doNotAcceptButton = menuButtonFactory.createButton("PRIVACY.OPT_IN.DO_NOT_ACCEPT_BUTTON", menuSkin, MenuButtonFactory.ButtonStyle.BTN_DIALOG_2)
				.withAction(() -> {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
					messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, Boolean.FALSE);
				})
				.build();

		acceptButton.getActor().getLabelCell().padLeft(10f).padRight(10f);
		doNotAcceptButton.getActor().getLabelCell().padLeft(10f).padRight(10f);

		menuTable.add(acceptButton).spaceTop(62f).row();
		menuTable.add(doNotAcceptButton).spaceTop(46f).row();

		menuTable.layout();

		Cell<Container<TextButton>> doNotAcceptCell = menuTable.getCell(doNotAcceptButton);
		acceptButton.size(doNotAcceptCell.getPrefWidth() * 1.2f, doNotAcceptCell.getPrefHeight() * 1.2f);
		doNotAcceptButton.setScale(0.94f);
	}

	private String translate(String i18nKey) {
		return i18nTranslator.getTranslatedString(i18nKey).toString();
	}

	@Override
	public void rebuildUI() {
		reset();
	}
}
