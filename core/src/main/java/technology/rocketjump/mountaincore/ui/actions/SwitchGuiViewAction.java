package technology.rocketjump.mountaincore.ui.actions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.ui.views.GuiViewName;

public class SwitchGuiViewAction implements ButtonAction {

	protected final GuiViewName targetViewName;
	protected final MessageDispatcher messageDispatcher;

	public SwitchGuiViewAction(GuiViewName targetViewName, MessageDispatcher messageDispatcher) {
		this.targetViewName = targetViewName;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void onClick() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, targetViewName);
	}
}
