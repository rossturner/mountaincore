package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;

import java.util.Arrays;
import java.util.List;

@Singleton
public class PrioritiesGuiView implements GuiView, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final ButtonFactory buttonFactory;
	private final Table layoutTable = new Table();

	@Inject
	public PrioritiesGuiView(MessageDispatcher messageDispatcher, ButtonFactory buttonFactory) {
		this.messageDispatcher = messageDispatcher;
		this.buttonFactory = buttonFactory;

		layoutTable.setTouchable(Touchable.enabled);
		layoutTable.defaults().padRight(28f);
		layoutTable.padLeft(23f);
		layoutTable.padBottom(17f);
	}

	@Override
	public void rebuildUI() {
		layoutTable.clearChildren();

		Button backButton = buttonFactory.buildDrawableButton("btn_back", "GUI.BACK_LABEL", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
		});
		layoutTable.add(backButton);

		List<JobPriority> prioritiesLowToHigh = Arrays.asList(JobPriority.LOWEST, JobPriority.LOWER, JobPriority.NORMAL, JobPriority.HIGHER, JobPriority.HIGHEST);
		for (JobPriority jobPriority : prioritiesLowToHigh) {
			Button button = buttonFactory.buildDrawableButton(jobPriority.buttonDrawableName, jobPriority.i18nKey, () -> {
				messageDispatcher.dispatchMessage(MessageType.REPLACE_JOB_PRIORITY, jobPriority);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.SET_JOB_PRIORITY);
			});
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
