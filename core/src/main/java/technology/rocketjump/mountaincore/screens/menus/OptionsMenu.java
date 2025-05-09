package technology.rocketjump.mountaincore.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.screens.menus.options.OptionsTab;
import technology.rocketjump.mountaincore.screens.menus.options.OptionsTabName;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.LabelFactory;
import technology.rocketjump.mountaincore.ui.widgets.MenuButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.WidgetFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.mountaincore.persistence.UserPreferences.PreferenceKey.CRASH_REPORTING;
import static technology.rocketjump.mountaincore.persistence.UserPreferences.PreferenceKey.ENABLE_TUTORIAL;

@Singleton
public class OptionsMenu extends BannerMenu implements DisplaysText {

	//	private final Texture twitchLogo;
	private final I18nTranslator i18nTranslator;
	private final UserPreferences userPreferences;
	private final WidgetFactory widgetFactory;
	private final LabelFactory labelFactory;
	private final SoundAssetDictionary soundAssetDictionary;

	private final Map<OptionsTabName, OptionsTab> tabs = new EnumMap<>(OptionsTabName.class);
	private OptionsTabName currentTab = OptionsTabName.GRAPHICS;

	@Inject
	public OptionsMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory, SoundAssetDictionary soundAssetDictionary,
	                   MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator, UserPreferences userPreferences,
	                   WidgetFactory widgetFactory, LabelFactory labelFactory) {
		super(skinRepository, menuButtonFactory, messageDispatcher, i18nTranslator);
		this.soundAssetDictionary = soundAssetDictionary;
//		twitchLogo = new Texture("assets/ui/TwitchGlitchPurple.png");

		this.i18nTranslator = i18nTranslator;
		this.userPreferences = userPreferences;
		this.widgetFactory = widgetFactory;
		this.labelFactory = labelFactory;
	}

	@Override
	public void reset() {
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
	protected Actor getMainBannerLogo() {
		Label titleRibbon = labelFactory.titleRibbon("MENU.OPTIONS");
		titleRibbon.setWidth(1132f);
		Table titleRibbonTable = new Table();
		titleRibbonTable.add(titleRibbon).width(1132f);

		return titleRibbonTable;
	}

	@Override
	protected void addMainBannerComponents(Table mainBanner) {
		Table buttonsTable = new Table();
		buttonsTable.defaults().uniformX();

		for (OptionsTabName tab : OptionsTabName.displayedOptionsTabs()) {

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
		crashReportCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(CRASH_REPORTING)));
		crashReportCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.CRASH_REPORTING_OPT_IN_MODIFIED, crashReportCheckbox.isChecked());
			}
			return true;
		});

		CheckBox tutorialCheckbox = widgetFactory.createLeftLabelledCheckbox("GUI.OPTIONS.MISC.TUTORIAL_ENABLED", menuSkin, 524f);
		tutorialCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		tutorialCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ENABLE_TUTORIAL)));
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
