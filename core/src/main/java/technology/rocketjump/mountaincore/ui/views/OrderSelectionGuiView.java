package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.ui.GameInteractionMode;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;

@Singleton
public class OrderSelectionGuiView implements GuiView, DisplaysText {

	private final ButtonFactory buttonFactory;
	private final Table layoutTable = new Table();
	private final MessageDispatcher messageDispatcher;

	@Inject
	public OrderSelectionGuiView(ButtonFactory buttonFactory, MessageDispatcher messageDispatcher) {
		this.buttonFactory = buttonFactory;
		this.messageDispatcher = messageDispatcher;

		layoutTable.setTouchable(Touchable.enabled);
		layoutTable.defaults().padRight(28f);
		layoutTable.padLeft(23f);
		layoutTable.padBottom(17f);

		rebuildUI();
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ORDER_SELECTION;
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
	public void rebuildUI() {
		layoutTable.clearChildren();

		Button backButton = buildButton("btn_back", "GUI.BACK_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName()));
		layoutTable.add(backButton);

		Button buildButton = buildButton("btn_current_orders_mine", "GUI.ORDERS.MINE",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_MINING));
		layoutTable.add(buildButton);

		Button chopButton = buildButton("btn_current_orders_chop", "GUI.ORDERS.CHOP_WOOD",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_CHOP_WOOD));
		layoutTable.add(chopButton);

		Button clearButton = buildButton("btn_current_orders_clear", "GUI.ORDERS.CLEAR_GROUND",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_CLEAR_GROUND));
		layoutTable.add(clearButton);

		Button extinguishButton = buildButton("btn_current_orders_extinguish", "GUI.ORDERS.EXTINGUISH_FLAMES",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_EXTINGUISH_FLAMES));
		layoutTable.add(extinguishButton);

		Button removeDesignationButton = buildButton("btn_current_orders_cancel", "GUI.CANCEL_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.CANCEL));
		layoutTable.add(removeDesignationButton);
	}

	private Button buildButton(String drawableName, String tooltipI18nKey, Runnable onClick) {
		return buttonFactory.buildDrawableButton(drawableName, tooltipI18nKey, onClick);
	}
}
