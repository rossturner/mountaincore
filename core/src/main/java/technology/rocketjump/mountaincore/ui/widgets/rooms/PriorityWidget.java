package technology.rocketjump.mountaincore.ui.widgets.rooms;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.common.collect.Lists;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PriorityWidget extends Table {

	private final List<PriorityButton> priorityButtons = new ArrayList<>();
	private Prioritisable prioritisableComponent;

	public PriorityWidget(Prioritisable prioritisableComponent, Skin skin, TooltipFactory tooltipFactory, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		this.prioritisableComponent = prioritisableComponent;
		this.defaults().pad(8);

		for (JobPriority priority : Lists.reverse(Arrays.asList(JobPriority.values()))) {
			PriorityButton priorityButton = new PriorityButton(priority, skin, tooltipFactory, messageDispatcher, soundAssetDictionary, () -> {
				setPriority(priority);
			});
			priorityButtons.add(priorityButton);
			this.add(priorityButton);
		}
		setPriority(prioritisableComponent.getPriority());
	}

	private void setPriority(JobPriority priority) {
		for (PriorityButton priorityButton : priorityButtons) {
			priorityButton.setChecked(priority.equals(priorityButton.getPriority()));
		}
		prioritisableComponent.setPriority(priority);
	}

}
