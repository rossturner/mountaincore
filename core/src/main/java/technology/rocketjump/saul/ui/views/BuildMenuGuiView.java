package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;

import static technology.rocketjump.saul.ui.views.GuiViewName.*;

@Singleton
public class BuildMenuGuiView implements GuiView, DisplaysText {

	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final TooltipFactory tooltipFactory;
	private final RoomTypeDictionary roomTypeDictionary;

	private Button backButton;
	private Table mainTable;
	private Table buttonsTable;

	@Inject
	public BuildMenuGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							RoomTypeDictionary roomTypeDictionary, I18nTranslator i18nTranslator,
							TooltipFactory tooltipFactory, I18nWidgetFactory i18NWidgetFactory) {

		skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.tooltipFactory = tooltipFactory;
		this.roomTypeDictionary = roomTypeDictionary;

	}

	@Override
	public void rebuildUI() {
		backButton = new Button(skin.getDrawable("btn_back"));
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
			}
		});
		backButton.addListener(new ChangeCursorOnHover(backButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(backButton, "GUI.BACK_LABEL", TooltipLocationHint.ABOVE);

		mainTable = new Table();
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.getDrawable("asset_bg_medium"));
		mainTable.pad(20);

		Container<Label> headerContainer = new Container<>();
		headerContainer.setBackground(skin.getDrawable("asset_bg_ribbon_title"));
		Label headerLabel = new Label(i18nTranslator.getTranslatedString("GUI.BUILD_LABEL").toString(), skin.get("title-header", Label.LabelStyle.class));
		headerContainer.setActor(headerLabel);
		headerContainer.center();

		mainTable.add(headerContainer).center().expandY().padBottom(20).row();

		buttonsTable = new Table();

		addButton("GUI.BUILD.FLOOR", "btn_build_icon_flooring", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, BUILD_FLOORING);
		});
		addButton("GUI.BUILD.WALLS", "btn_build_icon_walls", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, BUILD_WALLS);
		});
		addButton("GUI.BUILD.ROOFING", "btn_build_icon_roofing", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.ROOFING_INFO);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, BUILD_ROOFING);
		});
		addButton("GUI.BUILD.DOORS", "btn_build_icon_doors", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, BUILD_DOORS);
		});
		addButton("GUI.BUILD.BRIDGE", "btn_build_icon_bridge", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, BUILD_BRIDGE);
		});
		addButton("GUI.BUILD.PILLARS", "btn_build_icon_collumn", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, BUILD_PILLAR);
		});

		mainTable.add(buttonsTable).padLeft(30).padRight(30).padBottom(50).center().row();
	}

	private void addButton(String i18nKey, String drawableName, Runnable onClick) {
		Drawable drawable = skin.getDrawable(drawableName);
		Button actionButton = new Button(drawable);
		actionButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				onClick.run();
			}
		});
		actionButton.addListener(new ChangeCursorOnHover(actionButton, GameCursor.SELECT, messageDispatcher));
		tooltipFactory.simpleTooltip(actionButton, i18nKey, TooltipLocationHint.ABOVE);

		Container<Button> buttonContainer = new Container<>();
//		buttonContainer.setBackground(skin.getDrawable("room_bg_small"));
		buttonContainer.pad(10);
		buttonContainer.setActor(actionButton);

		buttonsTable.add(buttonContainer);
	}


	@Override
	public GuiViewName getName() {
		return BUILD_MENU;
	}

	@Override
	public GuiViewName getParentViewName() {
		return CONSTRUCTION_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.clear();
		containerTable.add(backButton).left().bottom().padLeft(30).padRight(50);
		containerTable.add(mainTable);
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}
}
