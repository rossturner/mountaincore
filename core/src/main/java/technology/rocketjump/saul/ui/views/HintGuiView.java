package technology.rocketjump.saul.ui.views;

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
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.GuiArea;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.hints.HintDictionary;
import technology.rocketjump.saul.ui.hints.HintProgressEvaluator;
import technology.rocketjump.saul.ui.hints.model.Hint;
import technology.rocketjump.saul.ui.hints.model.HintAction;
import technology.rocketjump.saul.ui.hints.model.HintProgress;
import technology.rocketjump.saul.ui.hints.model.HintProgressDescriptor;
import technology.rocketjump.saul.ui.i18n.I18nString;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.ui.hints.model.HintAction.HintActionType.DISMISS;
import static technology.rocketjump.saul.ui.hints.model.HintTrigger.HintTriggerType.ON_SETTLEMENT_SPAWNED;

@Singleton
public class HintGuiView implements GuiView, GameContextAware {

	private static final float UPDATE_PERIOD = 1.23f;
	private static final HintAction DISMISS_ACTION = new HintAction();
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final HintDictionary hintDictionary;
	private final HintProgressEvaluator hintProgressEvaluator;
	private final ButtonFactory buttonFactory;
	private final Skin uiSkin;
	private Table layoutTable;
	private GameContext gameContext;

	private List<Hint> displayedHints = new ArrayList<>();
	private List<HintProgress> currentProgress = new ArrayList<>();

	private float timeSinceLastUpdate = 0f;
	private final Label.LabelStyle defaultLabelStyle;

	static {
		DISMISS_ACTION.setButtonTextI18nKey("HINT.BUTTON.DISMISS");
		DISMISS_ACTION.setType(DISMISS);
	}

	@Inject
	public HintGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
					   I18nTranslator i18nTranslator,
					   HintDictionary hintDictionary,
					   HintProgressEvaluator hintProgressEvaluator, ButtonFactory buttonFactory) {
		this.messageDispatcher = messageDispatcher;
		this.uiSkin = guiSkinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.hintDictionary = hintDictionary;
		this.hintProgressEvaluator = hintProgressEvaluator;
		this.buttonFactory = buttonFactory;

		layoutTable = new Table(uiSkin);
		defaultLabelStyle = uiSkin.get("white_text_default-font-19", Label.LabelStyle.class);
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
			doUpdate();
		}
	}

	private void doUpdate() {
		currentProgress.clear();

		layoutTable.clearChildren();
		for (Hint displayedHint : displayedHints) {
			Table wrapperTable = new Table();
			wrapperTable.setBackground(uiSkin.get("asset_notifications_bg_patch", TenPatchDrawable.class));
			wrapperTable.setTouchable(Touchable.enabled);


			Button exitButton = new Button(uiSkin.getDrawable("btn_exit"));
			buttonFactory.attachClickCursor(exitButton, GameCursor.SELECT);
			exitButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.HINT_ACTION_TRIGGERED, DISMISS_ACTION);
					// On dismiss of initial tutorial, ensure all GUI areas are visible again
					for (GuiArea guiArea : GuiArea.values()) {
						messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_AREA, guiArea);
					}
				}
			});
			wrapperTable.add(exitButton).pad(10).left().top();

			Table hintTable = new Table();
			hintTable.defaults().padRight(60).padTop(20).padBottom(20);
			hintTable.padBottom(20);

			for (String i18nKey : displayedHint.getI18nKeys()) {
				I18nText text = i18nTranslator.getTranslatedString(i18nKey);
				Label label = new Label(text.toString(), defaultLabelStyle);
				label.setWrap(true);

				hintTable.add(label).left().width(1900).row();
			}

			boolean allowedToProgress = true;
			if (!displayedHint.getProgressDescriptors().isEmpty()) {
				allowedToProgress = buildProgressDescriptors(displayedHint, hintTable);
			}

			buildActions(displayedHint, hintTable, allowedToProgress);

			wrapperTable.add(hintTable).padTop(40);
			layoutTable.add(wrapperTable).pad(40).row();
		}
	}

	public void add(Hint hint) {
		hintProgressEvaluator.clearContextRelatedState();
		displayedHints.add(hint);
		gameContext.getSettlementState().getCurrentHints().add(hint.getHintId());
		doUpdate();
	}

	public void dismissAll() {
		for (Hint hint : new ArrayList<>(displayedHints)) {
			remove(hint);
		}
		doUpdate();
	}

	public void dismissAllExceptOnStart() {
		for (Hint hint : new ArrayList<>(displayedHints)) {
			if (hint.getTriggers().stream().noneMatch(t -> t.getTriggerType().equals(ON_SETTLEMENT_SPAWNED))) {
				remove(hint);
			}
		}
		doUpdate();
	}

	private void remove(Hint hint) {
		displayedHints.remove(hint);
		gameContext.getSettlementState().getCurrentHints().remove(hint.getHintId());
		gameContext.getSettlementState().getPreviousHints().put(hint.getHintId(), true);
		doUpdate();
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
		dismissAllExceptOnStart();
	}

	private boolean buildProgressDescriptors(Hint hint, Table hintTable) {
		boolean allProgressComplete = true;
		for (HintProgressDescriptor progressDescriptor : hint.getProgressDescriptors()) {
			Table progressTable = new Table(uiSkin);

			HintProgress progress = hintProgressEvaluator.evaluate(progressDescriptor);
			currentProgress.add(progress);
			if (!progress.isComplete()) {
				allProgressComplete = false;
			}

			if (progressDescriptor.isDisplayAsCheckbox()) {
				CheckBox.CheckBoxStyle checkBoxStyle = uiSkin.get("tutorial", CheckBox.CheckBoxStyle.class);
				CheckBox checkBox = new CheckBox(" " + i18nTranslator.getTranslatedString(progressDescriptor.getI18nKey()).toString(), checkBoxStyle);
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
				progressTable.add(new Label(text.toString(), defaultLabelStyle));
			}
			hintTable.add(progressTable).left().row();

		}

		return allProgressComplete;
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
		Label label = new Label(i18nTranslator.getTranslatedString(i18nKey).toString(), defaultLabelStyle);
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
}
