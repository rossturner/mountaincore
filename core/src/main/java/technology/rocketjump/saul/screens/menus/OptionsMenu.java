package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.screens.menus.options.OptionsTab;
import technology.rocketjump.saul.screens.menus.options.OptionsTabName;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.ScaledToFitLabel;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.CRASH_REPORTING;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.ENABLE_TUTORIAL;

@Singleton
public class OptionsMenu extends BannerMenu implements DisplaysText {

	//	private final Texture twitchLogo;
	private final I18nTranslator i18nTranslator;
	private final UserPreferences userPreferences;
	private final WidgetFactory widgetFactory;
	private final SoundAssetDictionary soundAssetDictionary;

	private final Map<OptionsTabName, OptionsTab> tabs = new EnumMap<>(OptionsTabName.class);
	private OptionsTabName currentTab = OptionsTabName.GRAPHICS;

	@Inject
	public OptionsMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory, SoundAssetDictionary soundAssetDictionary,
					   MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator, UserPreferences userPreferences,
					   WidgetFactory widgetFactory) {
		super(skinRepository, menuButtonFactory, messageDispatcher, i18nTranslator);
		this.soundAssetDictionary = soundAssetDictionary;
//		twitchLogo = new Texture("assets/ui/TwitchGlitchPurple.png");

		this.i18nTranslator = i18nTranslator;
		this.userPreferences = userPreferences;
		this.widgetFactory = widgetFactory;
	}

	@Override
	public void reset() {
//		menuTable.clearChildren();

//		OptionsTab currentTab = tabs.get(this.currentTab);
//		if (currentTab == null) {
//			Logger.error("No tab for name " + this.currentTab.name());
//		} else {
//			currentTab.populate(menuTable);
//		}
	}

	@Override
	public void savedGamesUpdated() {

	}

	@Override
	protected void addSecondaryBannerComponents(Table secondaryBanner) {
		if (currentTab != null) {
			tabs.get(currentTab).populate(secondaryBanner);
		}
	}

	@Override
	protected void addMainBannerComponents(Table mainBanner) {
		Label titleRibbon = new ScaledToFitLabel(i18nTranslator.getTranslatedString("MENU.OPTIONS").toString(), menuSkin, "title_ribbon", 1132);
		titleRibbon.setAlignment(Align.center);
//		mainBanner.add(titleRibbon).width(1132).row(); //TODO: title ribbon stretches out the banner and looks like it overlaps this banner and the minor banner

		Table buttonsTable = new Table();
		buttonsTable.defaults().uniformX();
		buttonsTable.debugAll();

		for (OptionsTabName tab : OptionsTabName.values()) {

			Container<TextButton> tabButton = menuButtonFactory.createButton(tab.getI18nKey(), menuSkin, MenuButtonFactory.ButtonStyle.BTN_SMALL_1_50PT)
					.withAction(() -> {
						this.currentTab = tab;
						this.rebuildUI();//todo: this right?
					})
					.build();

			//TODO: add twitch logo?
//			if (tab.equals(OptionsTabName.TWITCH)) {
//				Image twitchImage = new Image(twitchLogo);
//				tabButton.add(twitchImage).pad(2);
//			}


			buttonsTable.add(tabButton).spaceBottom(44f).row();
		}

		CheckBox crashReportCheckbox = widgetFactory.createLeftLabelledCheckbox("GUI.OPTIONS.MISC.CRASH_REPORTING_ENABLED", menuSkin, 524f);
		crashReportCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		crashReportCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(CRASH_REPORTING, "true")));
		crashReportCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, crashReportCheckbox.isChecked());
			}
			return true;
		});

		CheckBox tutorialCheckbox = widgetFactory.createLeftLabelledCheckbox("GUI.OPTIONS.MISC.TUTORIAL_ENABLED", menuSkin, 524f);
		tutorialCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		tutorialCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ENABLE_TUTORIAL, "true")));
		tutorialCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				userPreferences.setPreference(ENABLE_TUTORIAL, String.valueOf(tutorialCheckbox.isChecked()));
			}
			return true;
		});

		buttonsTable.add(crashReportCheckbox).fillX().padBottom(46f).row();
		buttonsTable.add(tutorialCheckbox).fillX().padBottom(142f).row();

		mainBanner.add(buttonsTable).row();


		Container<TextButton> backButton = menuButtonFactory.createButton("GUI.BACK_LABEL", menuSkin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE_50PT)
				.withAction(() -> {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
				})
				.build();

		backButton.width(330f);
		mainBanner.add(backButton).fill().row();
	}

	@Override
	public void show() {
		this.currentTab = null;
		rebuildUI();
	}

	@Override
	public void hide() {

	}

	public void setTabImplementations(List<OptionsTab> optionsTabClasses) {
		for (OptionsTab optionsTabClass : optionsTabClasses) {
			this.tabs.put(optionsTabClass.getTabName(), optionsTabClass);
		}
	}

	public void setCurrentTab(OptionsTabName currentTab) {
		this.currentTab = currentTab;
		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		super.rebuild();
	}

	@Override
	protected String getSecondaryBannerTitleI18nKey() {
		if (currentTab == null) {
			return null;
		}
		return currentTab.getI18nKey();
	}
}
