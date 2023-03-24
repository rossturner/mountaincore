package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.ui.GameInteractionMode;
import technology.rocketjump.mountaincore.ui.GameViewMode;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;

import static technology.rocketjump.mountaincore.ui.views.GuiViewName.BUILD_ROOFING;
import static technology.rocketjump.mountaincore.ui.views.GuiViewName.CONSTRUCTION_MENU;

@Singleton
public class BuildRoofingGuiView implements GuiView, DisplaysText {

	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final TooltipFactory tooltipFactory;
	private final ButtonFactory buttonFactory;

	private final Table layoutTable = new Table();

	@Inject
	public BuildRoofingGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							   TooltipFactory tooltipFactory, ButtonFactory buttonFactory) {

		skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.tooltipFactory = tooltipFactory;
		this.buttonFactory = buttonFactory;

		layoutTable.setTouchable(Touchable.enabled);
		layoutTable.defaults().padRight(28f);
		layoutTable.padLeft(23f);
		layoutTable.padBottom(17f);

		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		layoutTable.clearChildren();

		Button backButton = buildButton("btn_back", "GUI.BACK_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName()));
		layoutTable.add(backButton);

		Button addRoofButton = buildButton("btn_bottom_roofing", "GUI.ROOFING.ADD",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_ROOFING));
		layoutTable.add(addRoofButton);

		Button deconstructButton = buildButton("btn_construction_deconstruct", "GUI.DECONSTRUCT_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DECONSTRUCT));
		layoutTable.add(deconstructButton);

		Button cancelButton = buildButton("btn_current_orders_cancel", "GUI.CANCEL_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.CANCEL));
		layoutTable.add(cancelButton);
	}

	private Button buildButton(String drawableName, String tooltipI18nKey, Runnable onClick) {
		return buttonFactory.buildDrawableButton(drawableName, tooltipI18nKey, onClick);
	}

	@Override
	public GuiViewName getName() {
		return BUILD_ROOFING;
	}

	@Override
	public GuiViewName getParentViewName() {
		return CONSTRUCTION_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(layoutTable);
	}

	@Override
	public void onShow() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.ROOFING_INFO);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_ROOFING);
	}

	@Override
	public void onHide() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}
}
