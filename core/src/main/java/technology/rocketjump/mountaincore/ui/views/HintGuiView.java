package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.SteamUtils;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.ui.GuiArea;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.hints.HintDictionary;
import technology.rocketjump.mountaincore.ui.hints.HintProgressEvaluator;
import technology.rocketjump.mountaincore.ui.hints.model.*;
import technology.rocketjump.mountaincore.ui.i18n.*;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.GameDialogDictionary;
import technology.rocketjump.mountaincore.ui.widgets.ModalDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.mountaincore.persistence.UserPreferences.PreferenceKey.ENABLE_TUTORIAL;
import static technology.rocketjump.mountaincore.ui.hints.model.HintAction.HintActionType.DISMISS;

@Singleton
public class HintGuiView implements GuiView, GameContextAware, DisplaysText {

	private static final float UPDATE_PERIOD = 1.23f;
	private static final HintAction DISMISS_ACTION = new HintAction();
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final HintDictionary hintDictionary;
	private final HintProgressEvaluator hintProgressEvaluator;
	private final ButtonFactory buttonFactory;
	private final Skin uiSkin;
	private final GameDialogDictionary gameDialogDictionary;
	private final UserPreferences userPreferences;
	private Table layoutTable;
	private GameContext gameContext;
	private boolean hintsMinimised;

	private List<Hint> displayedHints = new ArrayList<>();
	private List<HintProgress> currentProgress = new ArrayList<>();

	private float timeSinceLastUpdate = 0f;


	static {
		DISMISS_ACTION.setButtonTextI18nKey("HINT.BUTTON.DISMISS");
		DISMISS_ACTION.setType(DISMISS);
	}

	@Inject
	public HintGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
					   I18nTranslator i18nTranslator,
					   HintDictionary hintDictionary,
					   HintProgressEvaluator hintProgressEvaluator, ButtonFactory buttonFactory, GameDialogDictionary gameDialogDictionary,
					   UserPreferences userPreferences) {
		this.messageDispatcher = messageDispatcher;
		this.uiSkin = guiSkinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.hintDictionary = hintDictionary;
		this.hintProgressEvaluator = hintProgressEvaluator;
		this.buttonFactory = buttonFactory;
		this.gameDialogDictionary = gameDialogDictionary;
		this.userPreferences = userPreferences;

		layoutTable = new Table(uiSkin);
	}


	@Override
	public void populate(Table containerTable) {
		update();
		containerTable.add(this.layoutTable);
	}

	@Override
	public void update() {
		if (gameContext != null) {

			timeSinceLastUpdate += Gdx.graphics.getDeltaTime();
			if (timeSinceLastUpdate > UPDATE_PERIOD) {
				timeSinceLastUpdate = 0f;
				checkForUpdate();
			}
		}
	}

	private void checkForUpdate() {
		List<HintProgress> newProgress = new ArrayList<>();
		for (Hint displayedHint : displayedHints) {
			for (HintProgressDescriptor descriptor : displayedHint.getProgressDescriptors()) {
				newProgress.add(hintProgressEvaluator.evaluate(descriptor));
			}
		}

		if (!newProgress.equals(currentProgress)) {
			rebuildUI();
		}
	}

	@Override
	public void rebuildUI() {
		currentProgress.clear();

		layoutTable.clearChildren();
		if (hintsMinimised && !displayedHints.isEmpty()) {
			Button button = buildButton("TUTORIAL.SHOW_LABEL", true, () -> {
				HintGuiView.this.hintsMinimised = false;
				HintGuiView.this.rebuildUI();
			});
			layoutTable.add(button).pad(40).row();
		} else {

			I18nWordClass wordClass = determineWordClass();
			for (Hint displayedHint : displayedHints) {
				Table tableWithBackground = new Table();
				tableWithBackground.setBackground(uiSkin.get("asset_notifications_bg_patch", TenPatchDrawable.class));
				tableWithBackground.setTouchable(Touchable.enabled);

				Table upperLeftButtonsTable = new Table();

				Button exitButton = new Button(uiSkin.getDrawable("btn_exit"));
				buttonFactory.attachClickCursor(exitButton, GameCursor.SELECT);
				exitButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						if (displayedHint.getHintId().contains("tutorial")) {
							ModalDialog dialog = gameDialogDictionary.confirmDismissTutorial(() -> {
								messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, DISMISS_ACTION);
								userPreferences.setPreference(ENABLE_TUTORIAL, "false");
								messageDispatcher.dispatchMessage(MessageType.PREFERENCE_CHANGED, ENABLE_TUTORIAL);
								// On dismiss of initial tutorial, ensure all GUI areas are visible again
								for (GuiArea guiArea : GuiArea.values()) {
									messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_AREA, guiArea);
								}
							});
							messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
						} else {
							messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, DISMISS_ACTION);
						}
					}
				});

				Button minimiseButton = new Button(uiSkin.getDrawable("btn_minimise_tutorial"));
				buttonFactory.attachClickCursor(minimiseButton, GameCursor.SELECT);
				minimiseButton.addListener(new ClickListener() {
					@Override
					public void clicked(InputEvent event, float x, float y) {
						HintGuiView.this.hintsMinimised = true;
						HintGuiView.this.rebuildUI();
					}
				});

				upperLeftButtonsTable.add(minimiseButton).padRight(20);
				upperLeftButtonsTable.add(exitButton);

				tableWithBackground.add(new Container<>());
				tableWithBackground.add(upperLeftButtonsTable).pad(10).right().top().row();

				Table hintTable = new Table();
				hintTable.defaults().padRight(40).padLeft(40).padTop(20).padBottom(20);

				for (String i18nKey : displayedHint.getI18nKeys()) {
					I18nText text = i18nTranslator.getTranslatedString(i18nKey, wordClass);
					Label label = new Label(text.toString(), defaultLabelStyle());
					label.setWrap(true);

					hintTable.add(label).left().width(1900).row();
				}

				boolean allowedToProgress = true;
				if (!displayedHint.getProgressDescriptors().isEmpty()) {
					allowedToProgress = buildProgressDescriptors(displayedHint, hintTable);
				}

				buildActions(displayedHint, hintTable, allowedToProgress);

				tableWithBackground.add(hintTable).colspan(2).padBottom(30);
				layoutTable.add(tableWithBackground).pad(40).row();
			}
		}
	}

	public void add(Hint hint) {
		hintProgressEvaluator.clearContextRelatedState();
		displayedHints.add(hint);
		gameContext.getSettlementState().getCurrentHints().add(hint.getHintId());
		hintsMinimised = false;
		rebuildUI();
	}

	public void dismissAll() {
		for (Hint hint : new ArrayList<>(displayedHints)) {
			remove(hint);
		}
		rebuildUI();
	}

	public void dismissAllExceptOnStart() {
		for (Hint hint : new ArrayList<>(displayedHints)) {
			if (hint.getTriggers().stream().noneMatch(t -> t.getTriggerType().equals(HintTrigger.HintTriggerType.ON_SETTLEMENT_SPAWNED))) {
				remove(hint);
			}
		}
		rebuildUI();
	}

	private void remove(Hint hint) {
		displayedHints.remove(hint);
		gameContext.getSettlementState().getCurrentHints().remove(hint.getHintId());
		gameContext.getSettlementState().getPreviousHints().put(hint.getHintId(), true);
		rebuildUI();
	}

	public List<Hint> getDisplayedHints() {
		return displayedHints;
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.HINTS;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		for (String currentHintId : gameContext.getSettlementState().getCurrentHints()) {
			Hint hint = hintDictionary.getById(currentHintId);
			if (hint == null) {
				Logger.error("Could not find hint with ID " + currentHintId);
			} else {
				this.displayedHints.add(hint);
			}
		}
	}

	@Override
	public void clearContextRelatedState() {
		hintsMinimised = false;
		dismissAllExceptOnStart();
	}

	private boolean buildProgressDescriptors(Hint hint, Table hintTable) {
		boolean allProgressComplete = true;
		I18nWordClass wordClass = determineWordClass();
		for (HintProgressDescriptor progressDescriptor : hint.getProgressDescriptors()) {
			if (isApplicable(progressDescriptor)) {
				Table progressTable = new Table(uiSkin);

				HintProgress progress = hintProgressEvaluator.evaluate(progressDescriptor);
				currentProgress.add(progress);
				if (!progress.isComplete()) {
					allProgressComplete = false;
				}

				if (progressDescriptor.isDisplayAsCheckbox()) {
					CheckBox.CheckBoxStyle checkBoxStyle = uiSkin.get("tutorial", CheckBox.CheckBoxStyle.class);
					I18nText translatedString = i18nTranslator.getTranslatedString(progressDescriptor.getI18nKey(), wordClass);
					CheckBox checkBox = new CheckBox(" " + translatedString.toString(), checkBoxStyle);
					checkBox.setChecked(progress.isComplete());
					checkBox.setDisabled(true);
					progressTable.add(checkBox).padRight(25);
				} else {
					ProgressBar bar = new ProgressBar(0, progress.total, 1, false, uiSkin);
					bar.setValue(progress.current);
					bar.setDisabled(true);
					progressTable.add(bar).padRight(25);

					Map<String, I18nString> replacements = new HashMap<>();
					replacements.put("currentQuantity", new I18nText(String.valueOf(progress.current)));
					replacements.put("requiredQuantity", new I18nText(String.valueOf(progress.total)));
					replacements.put("targetDescription", progress.targetDescription);
					I18nText text = i18nTranslator.getTranslatedWordWithReplacements("TUTORIAL.PROGRESS_DESCRIPTION.TEXT", replacements);
					progressTable.add(new Label(text.toString(), defaultLabelStyle()));
				}
				hintTable.add(progressTable).left().row();
			}
		}

		return allProgressComplete;
	}

	private I18nWordClass determineWordClass() {
		if (SteamUtils.isRunningOnSteamDeck()) {
			return I18nWordClass.STEAMDECK;
		} else {
			return I18nWordClass.UNSPECIFIED;
		}
	}

	private boolean isApplicable(HintProgressDescriptor progressDescriptor) {
		if (SteamUtils.isRunningOnSteamDeck()) {
			return !progressDescriptor.isIgnoreOnSteamDeck();
		} else {
			return true;
		}
	}

	private void buildActions(Hint displayedHint, Table hintTable, boolean allowedToProgress) {
		Table actionsTable = new Table(uiSkin);

		for (HintAction action : displayedHint.getActions()) {
			String i18nKey = action.getButtonTextI18nKey();
			if (i18nKey == null) {
				i18nKey = "HINT.BUTTON.NEXT";
			}
			Button button = buildButton(i18nKey, allowedToProgress, () -> {
				messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, action);
			});
			actionsTable.add(button).right().padLeft(15);
		}

//		if (displayedHint.isDismissable()) {
//			Button button = buildButton("HINT.BUTTON.DISMISS", () -> {
//				messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, DISMISS_ACTION);
//			});
//			actionsTable.add(button).right().padLeft(15);
//		}

		if (actionsTable.hasChildren()) {
			hintTable.add(actionsTable).right().row();
		}

	}

	private Button buildButton(String i18nKey, boolean allowedToProgress, Runnable onClick) {
		Button button = new Button(uiSkin);
		button.setDisabled(!allowedToProgress);
		Label label = new Label(i18nTranslator.getTranslatedString(i18nKey).toString(), defaultLabelStyle());
		button.add(label).padBottom(12).center();
		buttonFactory.attachClickCursor(button, GameCursor.SELECT);
		button.addListener(new ClickListener() {
			boolean triggeredOnce = false;

			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (allowedToProgress) {
					if (!triggeredOnce) {
						triggeredOnce = true;
						onClick.run();
					}
				}
			}
		});
		return button;
	}

	private Label.LabelStyle defaultLabelStyle() {
		return uiSkin.get("white_text_default-font-19", Label.LabelStyle.class);
	}
}
