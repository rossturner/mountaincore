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
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import static technology.rocketjump.saul.ui.GameInteractionMode.*;

@Singleton
public class OrderSelectionGuiView implements GuiView, DisplaysText {

	private final Skin skin;
	private final Table layoutTable = new Table();
	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;

	@Inject
	public OrderSelectionGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;

		layoutTable.setTouchable(Touchable.enabled);

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

		layoutTable.defaults().padLeft(24);

		Button backButton = buildButton("btn_back", "GUI.BACK_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName()));
		backButton.addListener(new ChangeCursorOnHover(GameCursor.SELECT, messageDispatcher));
		layoutTable.add(backButton).padLeft(40).padRight(30);

		Button mineButton = buildButton("btn_current_orders_mine", "GUI.ORDERS.MINE",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_MINING));
		layoutTable.add(mineButton);

		Button chopButton = buildButton("btn_current_orders_chop", "GUI.ORDERS.CHOP_WOOD",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_CHOP_WOOD));
		layoutTable.add(chopButton);

		Button clearButton = buildButton("btn_current_orders_clear", "GUI.ORDERS.CLEAR_GROUND",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_CLEAR_GROUND));
		layoutTable.add(clearButton);

		Button extinguishButton = buildButton("btn_current_orders_extinguish", "GUI.ORDERS.EXTINGUISH_FLAMES",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_EXTINGUISH_FLAMES));
		layoutTable.add(extinguishButton);

		Button removeDesignationButton = buildButton("btn_current_orders_cancel", "GUI.REMOVE_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, REMOVE_DESIGNATIONS));
		layoutTable.add(removeDesignationButton);

		// TODO up/down buttons to change view level
	}

	private Button buildButton(String drawableName, String tooltipI18nKey, Runnable onClick) {
		Button button = new Button(skin.getDrawable(drawableName));
		// select cursor on hover gets messy with different cursors for interaction modes
//		button.addListener(new ChangeCursorOnHover(GameCursor.SELECT, messageDispatcher));
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				onClick.run();
			}
		});
		tooltipFactory.simpleTooltip(button, tooltipI18nKey, TooltipLocationHint.ABOVE);
		return button;
	}
}
