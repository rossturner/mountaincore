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

import static technology.rocketjump.saul.ui.GameInteractionMode.DECONSTRUCT;
import static technology.rocketjump.saul.ui.GameInteractionMode.REMOVE_DESIGNATIONS;
import static technology.rocketjump.saul.ui.views.GuiViewName.BUILD_ROOFING;

@Singleton
public class ConstructionMenuGuiView implements GuiView, DisplaysText {

	private final Skin skin;
	private final Table layoutTable = new Table();
	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;

	@Inject
	public ConstructionMenuGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, TooltipFactory tooltipFactory) {
		this.skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;

		layoutTable.setTouchable(Touchable.enabled);
		layoutTable.defaults().padRight(28f);
		layoutTable.padLeft(23f);
		layoutTable.padBottom(17f);

		rebuildUI();
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.CONSTRUCTION_MENU;
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

		Button buildButton = buildButton("btn_construction_build", "GUI.BUILD_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.BUILD_MENU));
		layoutTable.add(buildButton);

		Button roofButton = buildButton("btn_bottom_roofing", "GUI.BUILD.ROOFING", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.ROOFING_INFO);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, BUILD_ROOFING);
		});
		layoutTable.add(roofButton);

		Button powerWaterButton = buildButton("btn_construction_power_and_water", "GUI.POWER_LABEL",
			() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.POWER_WATER_MENU));
		layoutTable.add(powerWaterButton);

		Button deconstructButton = buildButton("btn_construction_deconstruct", "GUI.DECONSTRUCT_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DECONSTRUCT));
		layoutTable.add(deconstructButton);

		Button cancelButton = buildButton("btn_current_orders_cancel", "GUI.CANCEL_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, REMOVE_DESIGNATIONS));
		layoutTable.add(cancelButton);
	}

	private Button buildButton(String drawableName, String tooltipI18nKey, Runnable onClick) {
		Button button = new Button(skin.getDrawable(drawableName));
		button.addListener(new ChangeCursorOnHover(button, GameCursor.SELECT, messageDispatcher));
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
