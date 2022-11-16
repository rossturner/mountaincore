package technology.rocketjump.saul.ui.widgets.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;

public class PriorityButton extends Container<Image> {

	private final Drawable backgroundDrawable;
	private final Drawable priorityDrawable;
	private final JobPriority priority;
	private boolean checked;

	public PriorityButton(JobPriority priority, Skin skin, TooltipFactory tooltipFactory, MessageDispatcher messageDispatcher, Runnable onClick) {

		// container background is when button is "checked"
		this.priority = priority;
		this.backgroundDrawable = skin.getDrawable("btn_crafting_priority");
		this.priorityDrawable = skin.getDrawable(priority.craftingDrawableName);
		Image priorityImage = new Image(priorityDrawable);

		priorityImage.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				onClick.run();
			}
		});
		priorityImage.addListener(new ChangeCursorOnHover(priorityImage, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(priorityImage, priority.i18nKey, TooltipLocationHint.ABOVE);

		this.setChecked(false);
		this.setActor(priorityImage);
	}

	public JobPriority getPriority() {
		return priority;
	}


	public void setChecked(boolean checked) {
		this.checked = checked;
		if (checked) {
			this.setBackground(backgroundDrawable);
		} else {
			this.setBackground(null);
		}
		this.padBottom(8);
	}

}
