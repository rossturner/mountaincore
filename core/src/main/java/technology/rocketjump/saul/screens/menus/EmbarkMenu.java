package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.factories.names.SettlementNameGenerator;
import technology.rocketjump.saul.messaging.InfoType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.StartNewGameMessage;
import technology.rocketjump.saul.persistence.SavedGameInfo;
import technology.rocketjump.saul.persistence.SavedGameStore;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.GameDialog;
import technology.rocketjump.saul.ui.widgets.GameDialogDictionary;
import technology.rocketjump.saul.ui.widgets.LabelFactory;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

@Singleton
public class EmbarkMenu extends PaperMenu implements DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final SavedGameStore savedGameStore;
	private final SettlementNameGenerator settlementNameGenerator;
	private final WidgetFactory widgetFactory;
	private final SoundAssetDictionary soundAssetDictionary;
	private final GameDialogDictionary gameDialogDictionary;
	private final LabelFactory labelFactory;
	private final Random random = new RandomXS128();
	private TextField nameInput;
	private TextField seedInput;
	private int selectedMapWidth;
	private int selectedMapHeight;
	private boolean peacefulMode = false;

	@Inject
	public EmbarkMenu(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
	                  I18nTranslator i18nTranslator, SavedGameStore savedGameStore,
	                  SettlementNameGenerator settlementNameGenerator, WidgetFactory widgetFactory,
	                  SoundAssetDictionary soundAssetDictionary, GameDialogDictionary gameDialogDictionary, LabelFactory labelFactory) {
		super(guiSkinRepository);
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.savedGameStore = savedGameStore;
		this.settlementNameGenerator = settlementNameGenerator;
		this.widgetFactory = widgetFactory;
		this.soundAssetDictionary = soundAssetDictionary;
		this.gameDialogDictionary = gameDialogDictionary;
		this.labelFactory = labelFactory;
	}

	@Override
	public void show() {
		reset();
	}

	@Override
	public void hide() {

	}

	@Override
	public void reset() {
		this.nameInput.setText(settlementNameGenerator.create(random.nextLong()));
		this.seedInput.setText(String.valueOf(Math.abs(random.nextLong())));
	}

	@Override
	public void savedGamesUpdated() {
		//todo: shouldn't be here
	}

	@Override
	protected Actor buildComponentLayer() {


		Label titleRibbon = labelFactory.titleRibbon("GUI.EMBARK.TITLE");
		titleRibbon.setWidth(1576f);
		Table titleTable = new Table();
		titleTable.add(titleRibbon).width(1576).padTop(13f);

		Label nameLabel = new Label(i18nTranslator.translate("GUI.EMBARK.SETTLEMENT_NAME"), skin, "embark_ribbon");
		nameLabel.setAlignment(Align.top);  //this uses padding to make it off center for aesthetics


		this.nameInput = new TextField("", skin);
		this.nameInput.addListener(new ChangeCursorOnHover(nameInput, GameCursor.I_BEAM, messageDispatcher));
		this.nameInput.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		this.nameInput.setAlignment(Align.center);
		Button randomiseNameButton = new Button(skin, "btn_random");
		randomiseNameButton.addListener(new ChangeCursorOnHover(randomiseNameButton, GameCursor.SELECT, messageDispatcher));
		randomiseNameButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		randomiseNameButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				nameInput.setText(settlementNameGenerator.create(random.nextLong()));
			}
		});


		Label mapSizeLabel = new Label(i18nTranslator.translate("GUI.EMBARK.MAP_SIZE"), skin, "embark_ribbon");
		mapSizeLabel.setAlignment(Align.top);  //this uses padding to make it off center for aesthetics

		//fudge, first one added to group is default checked
		ButtonGroup<ImageButton> mapRadioSelectionGroup = new ButtonGroup<>();
		Table mediumMap = buildMapButton(mapRadioSelectionGroup, "GUI.EMBARK.MAP_SIZE.MEDIUM", "medium_map_btn", 0.8f, 400, 300);
		Table smallMap = buildMapButton(mapRadioSelectionGroup, "GUI.EMBARK.MAP_SIZE.SMALL", "small_map_btn", 0.5f, 200, 150);
		Table largeMap = buildMapButton(mapRadioSelectionGroup, "GUI.EMBARK.MAP_SIZE.LARGE", "large_map_btn", 1.0f, 600, 450);


		Label seedLabel = new Label(i18nTranslator.translate("GUI.EMBARK.MAP_SEED"), skin, "embark_seed_label");
		this.seedInput = new TextField("", skin);
		this.seedInput.addListener(new ChangeCursorOnHover(this.seedInput, GameCursor.I_BEAM, messageDispatcher));
		this.seedInput.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		this.seedInput.setAlignment(Align.center);

		CheckBox peacefulModeCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.EMBARK.PEACEFUL_MODE", skin, 320);
		peacefulModeCheckbox.setChecked(peacefulMode);
		peacefulModeCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				this.peacefulMode = peacefulModeCheckbox.isChecked();
//				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
//				GlobalSettings.ZOOM_TO_CURSOR = zoomToCursorCheckbox.isChecked();
//				userPreferences.setPreference(UserPreferences.PreferenceKey.ZOOM_TO_CURSOR, String.valueOf(GlobalSettings.ZOOM_TO_CURSOR));
			}
			return true;
		});

		Button randomiseSeedButton = new Button(skin, "btn_random");
		randomiseSeedButton.addListener(new ChangeCursorOnHover(randomiseSeedButton, GameCursor.SELECT, messageDispatcher));
		randomiseSeedButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		randomiseSeedButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				seedInput.setText(String.valueOf(Math.abs(random.nextLong())));
			}
		});

		Button backButton = new Button(skin, "btn_embark_back");
		backButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		backButton.addListener(new ChangeCursorOnHover(backButton, GameCursor.SELECT, messageDispatcher));
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
			}
		});

		Button startButton = new Button(skin, "btn_embark_next");
		startButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary, "GameStart"));
		startButton.addListener(new ChangeCursorOnHover(startButton, GameCursor.SELECT, messageDispatcher));

		startButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				String settlementName = getSettlementName();
				SavedGameInfo existingSave = savedGameStore.getByName(settlementName);
				if (settlementName.isBlank()) {
					GameDialog dialog = gameDialogDictionary.createInfoDialog(skin, InfoType.SETTLEMENT_NAME_NOT_SPECIFIED, Collections.emptyMap());
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
				} else if (existingSave != null) {
					GameDialog dialog = gameDialogDictionary.createInfoDialog(skin, InfoType.SETTLEMENT_NAME_ALREADY_IN_USE, Map.of("name", new I18nWord(settlementName)));
					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.CONFIRM"), (Runnable) () -> {
						startGame();
					});
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
				} else {
					startGame();
				}
			}
		});

		//colspan of 3 to keep center aligned
		Table table = new Table();
		table.padLeft(36.0f);
		table.padRight(36.0f);
		table.padTop(0.0f);
		table.padBottom(0.0f);
		table.add(titleTable).width(1576f).padTop(84.0f).padBottom(84.0f).colspan(3).row();

		float ribbonWidth = 880;
		table.add().width(1100f).fillX();
		table.add(nameLabel).width(ribbonWidth).padTop(13f).spaceBottom(48f);
		table.add().width(1100f).fillX();
		table.row();

		table.add();
		table.add(nameInput).width(ribbonWidth).height(85f).spaceBottom(164f);
		table.add(randomiseNameButton).left().padLeft(40f).spaceBottom(164f);
		table.row();

		table.add();
		table.add(mapSizeLabel).width(ribbonWidth).padTop(13f).spaceBottom(60f);
		table.add();
		table.row();

		table.add(smallMap).fillY();
		table.add(mediumMap).fillY();
		table.add(largeMap).fillY();
		table.row();


		Table bottomLeftCorner = new Table();
		bottomLeftCorner.add(backButton).left();
		bottomLeftCorner.add(seedLabel).right().padRight(34f).expandX();

		Table bottomMiddle = new Table();
		bottomMiddle.add(seedInput).width(ribbonWidth).height(84f).spaceBottom(46f);
		bottomMiddle.row();
		bottomMiddle.add(new Container<>(peacefulModeCheckbox));

		Table bottomRightCorner = new Table();
		bottomRightCorner.add(randomiseSeedButton).left().padLeft(40f).expandX();
		bottomRightCorner.add(startButton).right();

		table.add(bottomLeftCorner).fillX();
		table.add(bottomMiddle).bottom();
		table.add(bottomRightCorner).fillX();
		table.row();

		return table;
	}

	private void startGame() {
		messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
		messageDispatcher.dispatchMessage(MessageType.START_NEW_GAME, new StartNewGameMessage(getSettlementName(), parseSeed(), selectedMapWidth, selectedMapHeight, peacefulMode));
	}

	private Table buildMapButton(ButtonGroup<ImageButton> mapRadioSelectionGroup, String labelI18nKey, String mapDrawableName, float scale, int mapWidth, int mapHeight) {
		Label sizeLabel = new Label(i18nTranslator.getTranslatedString(labelI18nKey).toString(), skin, "map_size_label");
		ImageButton mapButton = new ImageButton(skin, mapDrawableName);
		mapButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		mapButton.addListener(new ChangeCursorOnHover(mapButton, GameCursor.SELECT, messageDispatcher));
		mapButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (mapButton.isChecked()) {
					selectedMapWidth = mapWidth;
					selectedMapHeight = mapHeight;
				}
			}
		});
		Image image = mapButton.getImage();
		mapButton.getImageCell().size(image.getDrawable().getMinWidth() * scale, image.getDrawable().getMinHeight() * scale);
		Table layout = new Table();
		layout.add(mapButton).expandY().row();
		layout.add(sizeLabel);
		mapRadioSelectionGroup.add(mapButton);
		return layout;
	}

	private long parseSeed() {
		String seedText = seedInput.getText().trim();
		if (StringUtils.isNumeric(seedText)) {
			if (seedText.length() > 18) {
				seedText = seedText.substring(0, 18);
			}
			return Long.parseLong(seedText);
		} else {
			long hash = 0;
			for (char c : seedText.toCharArray()) {
				hash = 31L*hash + c;
			}
			return hash;
		}
	}

	private String getSettlementName() {
		return nameInput.getText().trim();
	}

	@Override
	public void rebuildUI() {
		rebuild();
	}


}
