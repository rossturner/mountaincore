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
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import java.util.List;

@Singleton
public class DefaultGuiView implements GuiView, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final Skin skin;
	private Table buttonsTable = new Table();

	@Inject
	public DefaultGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory) {
		this.messageDispatcher = messageDispatcher;
		this.skin = guiSkinRepository.getMainGameSkin();
		this.tooltipFactory = tooltipFactory;

		buttonsTable.setTouchable(Touchable.enabled);
		buttonsTable.defaults().padRight(14f);
		buttonsTable.padLeft(23f / 2f);
		buttonsTable.padBottom(17f / 2f);

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
				buildButton("btn_bottom_construction", "GUI.BUILD_LABEL", () -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.BUILD_MENU)),
				buildButton("btn_bottom_zones", "GUI.ZONES_LABEL", () -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_SELECTION)),
				buildButton("btn_bottom_priority", "GUI.PRIORITY_LABEL", () -> {
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.PRIORITY_MENU);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.JOB_PRIORITY);
				})
		)) {
			buttonsTable.add(button).size(button.getMinWidth() / 2f, button.getMinHeight() / 2f);
		}
	}

	private Button buildButton(String styleName, String i18nKey, Runnable onClick) {
		Button button = new Button(skin.get(styleName, Button.ButtonStyle.class));
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				onClick.run();
			}
		});
		button.addListener(new ChangeCursorOnHover(GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(button, i18nKey, TooltipLocationHint.ABOVE);
		return button;
	}
}
