package technology.rocketjump.saul.ui.widgets.constructions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.common.collect.Lists;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.widgets.rooms.PriorityButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Based on RoomPriorityWidget
 */
public class ConstructionPriorityWidget extends Table {

	private final List<PriorityButton> priorityButtons = new ArrayList<>();
	private final Construction construction;
	private final MessageDispatcher messageDispatcher;

	public ConstructionPriorityWidget(Construction construction, Skin skin, TooltipFactory tooltipFactory, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		this.construction = construction;
		this.messageDispatcher = messageDispatcher;
		this.defaults().pad(8);

		for (JobPriority priority : Lists.reverse(Arrays.asList(JobPriority.values()))) {
			PriorityButton priorityButton = new PriorityButton(priority, skin, tooltipFactory, messageDispatcher, soundAssetDictionary, () -> {
				if (priority.equals(JobPriority.DISABLED)) {
					messageDispatcher.dispatchMessage(MessageType.CANCEL_CONSTRUCTION, construction);
				} else {
					setPriority(priority);
				}
			});
			priorityButtons.add(priorityButton);
			this.add(priorityButton);
		}
		setPriority(construction.getPriority());
	}

	private void setPriority(JobPriority priority) {
		for (PriorityButton priorityButton : priorityButtons) {
			priorityButton.setChecked(priority.equals(priorityButton.getPriority()));
		}
		construction.setPriority(priority, messageDispatcher);
	}

}
