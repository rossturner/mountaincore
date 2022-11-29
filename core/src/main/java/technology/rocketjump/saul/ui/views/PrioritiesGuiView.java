package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import java.util.Arrays;
import java.util.List;

@Singleton
public class PrioritiesGuiView implements GuiView, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final Skin skin;
	private final TooltipFactory tooltipFactory;
	private final Table layoutTable = new Table();

	@Inject
	public PrioritiesGuiView(GuiSkinRepository skinRepository, MessageDispatcher messageDispatcher,
							 TooltipFactory tooltipFactory) {
		this.messageDispatcher = messageDispatcher;
		this.skin = skinRepository.getMainGameSkin();
		this.tooltipFactory = tooltipFactory;

		layoutTable.setTouchable(Touchable.enabled);
		layoutTable.defaults().padRight(28f);
		layoutTable.padLeft(23f);
		layoutTable.padBottom(17f);
	}

	@Override
	public void rebuildUI() {
		layoutTable.clearChildren();

		Button backButton = new Button(skin.getDrawable("btn_back"));
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
			}
		});
		backButton.addListener(new ChangeCursorOnHover(backButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(backButton, "GUI.BACK_LABEL", TooltipLocationHint.ABOVE);
		layoutTable.add(backButton);

		List<JobPriority> prioritiesLowToHigh = Arrays.asList(JobPriority.LOWEST, JobPriority.LOWER, JobPriority.NORMAL, JobPriority.HIGHER, JobPriority.HIGHEST);
		for (JobPriority jobPriority : prioritiesLowToHigh) {
			Button button = new Button(skin.getDrawable(jobPriority.buttonDrawableName));
			button.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.REPLACE_JOB_PRIORITY, jobPriority);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.SET_JOB_PRIORITY);
				}
			});
			button.addListener(new ChangeCursorOnHover(button, GameCursor.SELECT, messageDispatcher));
			tooltipFactory.simpleTooltip(button, jobPriority.i18nKey, TooltipLocationHint.ABOVE);
			layoutTable.add(button);
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.PRIORITY_MENU;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(layoutTable);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	@Override
	public void onHide() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
	}
}
