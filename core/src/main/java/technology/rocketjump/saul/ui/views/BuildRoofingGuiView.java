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
import technology.rocketjump.saul.rooms.RoomTypeDictionary;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;

import static technology.rocketjump.saul.ui.GameInteractionMode.CANCEL_ROOFING;
import static technology.rocketjump.saul.ui.GameInteractionMode.DECONSTRUCT_ROOFING;
import static technology.rocketjump.saul.ui.views.GuiViewName.BUILD_ROOFING;
import static technology.rocketjump.saul.ui.views.GuiViewName.CONSTRUCTION_MENU;

@Singleton
public class BuildRoofingGuiView implements GuiView, DisplaysText {

	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final TooltipFactory tooltipFactory;
	private final RoomTypeDictionary roomTypeDictionary;

	private boolean displayed;
	private final Table layoutTable = new Table();

	@Inject
	public BuildRoofingGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							   RoomTypeDictionary roomTypeDictionary, I18nTranslator i18nTranslator,
							   TooltipFactory tooltipFactory, I18nWidgetFactory i18NWidgetFactory) {

		skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.tooltipFactory = tooltipFactory;
		this.roomTypeDictionary = roomTypeDictionary;

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
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DECONSTRUCT_ROOFING));
		layoutTable.add(deconstructButton);

		Button cancelButton = buildButton("btn_current_orders_cancel", "GUI.CANCEL_LABEL",
				() -> messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, CANCEL_ROOFING));
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
		this.displayed = true;
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.ROOFING_INFO);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DESIGNATE_ROOFING);
	}

	@Override
	public void onHide() {
		this.displayed = false;
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}
}
