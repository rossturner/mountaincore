package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;

import java.util.List;

@Singleton
public class DefaultGuiView implements GuiView, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final ButtonFactory buttonFactory;
	private Table buttonsTable = new Table();

	@Inject
	public DefaultGuiView(MessageDispatcher messageDispatcher, ButtonFactory buttonFactory) {
		this.messageDispatcher = messageDispatcher;
		this.buttonFactory = buttonFactory;

		buttonsTable.setTouchable(Touchable.enabled);
		buttonsTable.defaults().padRight(28f);
		buttonsTable.padLeft(23f);
		buttonsTable.padBottom(17f);

		rebuildUI();
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(buttonsTable);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	@Override
	public void rebuildUI() {
		buttonsTable.clearChildren();

		for (Button button : List.of(
				buildButton("btn_bottom_orders", "GUI.ORDERS_LABEL", () -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ORDER_SELECTION)),
				buildButton("btn_bottom_construction", "GUI.CONSTRUCTION_LABEL", () -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.CONSTRUCTION_MENU)),
				buildButton("btn_bottom_zones", "GUI.ROOMS_LABEL", () -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_SELECTION)),
				buildButton("btn_bottom_priority", "GUI.PRIORITY_LABEL", () -> {
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.PRIORITY_MENU);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.JOB_PRIORITY);
				})
		)) {
			buttonsTable.add(button);
		}
	}

	private Button buildButton(String styleName, String i18nKey, Runnable onClick) {
		return buttonFactory.buildDrawableButton(styleName, i18nKey, onClick);
	}
}
