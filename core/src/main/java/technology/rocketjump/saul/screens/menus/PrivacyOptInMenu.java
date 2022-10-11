package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.LanguageType;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonStyle;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.IconButton;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class PrivacyOptInMenu implements Menu {

	public static final int NUM_PRIVACY_OPT_IN_LINES = 10;
	private final List<Label> textLabels = new ArrayList<>();
	private final IconButton acceptButton;
	private final IconButton doNotAcceptButton;

	private final Skin uiSkin;
	private final I18nRepo i18nRepo;
	private Table menuTable;
	private final I18nTranslator i18nTranslator;

	private final SelectBox<LanguageType> languageSelect;

	@Inject
	public PrivacyOptInMenu(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							IconButtonFactory iconButtonFactory, I18nTranslator i18nTranslator, I18nRepo i18nRepo,
							I18nWidgetFactory i18NWidgetFactory, TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository) {
		this.i18nTranslator = i18nTranslator;
		this.i18nRepo = i18nRepo;
		this.uiSkin = guiSkinRepository.getDefault();


		menuTable = new Table(uiSkin);
		menuTable.setFillParent(false);
		menuTable.center();
		menuTable.background("default-rect");

		for (int line = 1; line <= NUM_PRIVACY_OPT_IN_LINES; line++) {
			textLabels.add(i18NWidgetFactory.createLabel("PRIVACY.OPT_IN.LINE_"+line));
		}

		acceptButton = iconButtonFactory.create("PRIVACY.OPT_IN.ACCEPT_BUTTON", null, Color.LIGHT_GRAY, ButtonStyle.EXTRA_WIDE);
		acceptButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
			messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, Boolean.TRUE);
		});

		doNotAcceptButton = iconButtonFactory.create("PRIVACY.OPT_IN.DO_NOT_ACCEPT_BUTTON", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
		doNotAcceptButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
			messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, Boolean.FALSE);
		});

		this.languageSelect = buildLanguageSelect(messageDispatcher, i18nRepo, userPreferences, uiSkin, this, textureAtlasRepository, fontRepository, guiSkinRepository);
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

		Table languageRow = new Table(uiSkin);
		languageRow.add(new Image(i18nRepo.getCurrentLanguageType().getIconSprite()));
		languageRow.add(languageSelect).padLeft(5);
		menuTable.add(languageRow).row();

		for (Label label : textLabels) {
			menuTable.add(label).left().pad(0, 10, 0, 10).row();
		}

		menuTable.add(acceptButton).pad(10).row();
		menuTable.add(doNotAcceptButton).pad(10).row();
	}



	static SelectBox<LanguageType> buildLanguageSelect(MessageDispatcher messageDispatcher, I18nRepo i18nRepo,
													   UserPreferences userPreferences, Skin uiSkin, Menu parent,
													   TextureAtlasRepository textureAtlasRepository, FontRepository fontRepository,
													   GuiSkinRepository guiSkinRepository) {
		i18nRepo.init(textureAtlasRepository);
		String languageCode = userPreferences.getPreference(UserPreferences.PreferenceKey.LANGUAGE, "en-gb");
		List<LanguageType> allLanguages = i18nRepo.getAllLanguages();

		LanguageType selectedLanguage = null;
		for (LanguageType languageType : allLanguages) {
			if (languageType.getCode().equals(languageCode)) {
				selectedLanguage = languageType;
				break;
			}
		}
		if (selectedLanguage == null) {
			selectedLanguage = allLanguages.get(0);
		}

		SelectBox<LanguageType> languageSelect = new SelectBox<>(uiSkin);
		// Override font with unicode-guaranteed font to show east asian characters
		SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle(languageSelect.getStyle());
		style.font = fontRepository.getUnicodeFont().getBitmapFont();
		com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle listStyle = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(style.listStyle);
		listStyle.font = fontRepository.getUnicodeFont().getBitmapFont();
		style.listStyle = listStyle;
		languageSelect.setStyle(style);

		Array<LanguageType> languageEntries = new Array<>();
		for (LanguageType language : allLanguages) {
			languageEntries.add(language);
		}

		languageSelect.setItems(languageEntries);
		languageSelect.setSelected(selectedLanguage);
		languageSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				LanguageType selectedLanguage = languageSelect.getSelected();
				changeLanguage(selectedLanguage, userPreferences, fontRepository, i18nRepo, messageDispatcher, guiSkinRepository);
				parent.reset();
			}
		});

		return languageSelect;
	}

	public static void changeLanguage(LanguageType selectedLanguage, UserPreferences userPreferences,
									  FontRepository fontRepository, I18nRepo i18nRepo, MessageDispatcher messageDispatcher,
									  GuiSkinRepository guiSkinRepository) {
		userPreferences.setPreference(UserPreferences.PreferenceKey.LANGUAGE, selectedLanguage.getCode());
		fontRepository.changeFonts(selectedLanguage);
		guiSkinRepository.fontChanged();
		i18nRepo.setCurrentLanguage(selectedLanguage);
		messageDispatcher.dispatchMessage(MessageType.LANGUAGE_CHANGED);
	}
}
