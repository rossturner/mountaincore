package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonFactory;

import java.util.ArrayList;

import static technology.rocketjump.saul.ui.views.GuiViewName.STOCKPILE_SELECTION;

@Singleton
public class RoomSelectionGuiView implements GuiView, DisplaysText {

	private final int ITEMS_PER_ROW = 11;
	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final TooltipFactory tooltipFactory;
	private final RoomTypeDictionary roomTypeDictionary;
	private final ButtonFactory buttonFactory;

	private Button backButton;
	private Table mainTable;
	private Table buttonsTable;

	@Inject
	public RoomSelectionGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								RoomTypeDictionary roomTypeDictionary, I18nTranslator i18nTranslator,
								TooltipFactory tooltipFactory, ButtonFactory buttonFactory) {

		skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.tooltipFactory = tooltipFactory;
		this.roomTypeDictionary = roomTypeDictionary;

		this.buttonFactory = buttonFactory;
	}

	@Override
	public void rebuildUI() {
		backButton = buttonFactory.buildDrawableButton("btn_back", "GUI.BACK_LABEL", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
		});

		mainTable = new Table();
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.getDrawable("asset_dwarf_select_bg"));
		mainTable.pad(20);

		Container<Label> headerContainer = new Container<>();
		headerContainer.setBackground(skin.getDrawable("asset_bg_ribbon_title"));
		Label headerLabel = new Label(i18nTranslator.getTranslatedString("GUI.ROOMS_LABEL").toString(), skin.get("title-header", Label.LabelStyle.class));
		headerContainer.setActor(headerLabel);
		headerContainer.center();

		mainTable.add(headerContainer).center().expandY().padBottom(20).row();

		buttonsTable = new Table();

		int rowCursor = 0;
		ArrayList<RoomType> roomTypes = new ArrayList<>(roomTypeDictionary.getAll());
		roomTypes.sort((o1, o2) -> {
			String o1Translated = i18nTranslator.getTranslatedString(o1.getI18nKey()).toString();
			String o2Translated = i18nTranslator.getTranslatedString(o2.getI18nKey()).toString();
			return o1Translated.compareTo(o2Translated);
		});
		for (RoomType roomType : roomTypes) {

			Drawable drawable;
			try {
				drawable = skin.getDrawable(roomType.getDrawableName());
			} catch (GdxRuntimeException e) {
				// To handle when drawable not found
				drawable = skin.getDrawable("placeholder");
			}
			Button roomButton = new Button(drawable);
			buttonFactory.attachClickCursor(roomButton, GameCursor.SELECT);
			roomButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.GUI_ROOM_TYPE_SELECTED, roomType);
					if (roomType.isStockpile()) {
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, STOCKPILE_SELECTION);
					} else {
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_ROOM);
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_EDITING);
					}
				}
			});

			tooltipFactory.withTooltipText(roomButton, roomType.getI18nKey(), TooltipFactory.TooltipBackground.LARGE_PATCH_DARK);

			Container<Button> buttonContainer = new Container<>();
			buttonContainer.setBackground(skin.getDrawable("room_bg_small"));
			buttonContainer.pad(10);
			buttonContainer.setActor(roomButton);

			buttonsTable.add(buttonContainer);

			rowCursor++;
			if (rowCursor % ITEMS_PER_ROW == 0) {
				buttonsTable.row();
			}
		}

		while (rowCursor % ITEMS_PER_ROW != 0) {
			Image spacerImage = new Image(skin.getDrawable("room_bg_small"));
			buttonsTable.add(spacerImage).size(201, 201);
			rowCursor++;
		}


		mainTable.add(buttonsTable).padLeft(30).padRight(30).padBottom(50).center().row();
	}


	@Override
	public GuiViewName getName() {
		return GuiViewName.ROOM_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
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
