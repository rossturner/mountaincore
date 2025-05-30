package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ray3k.tenpatch.TenPatchDrawable;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;
import technology.rocketjump.mountaincore.ui.GameInteractionMode;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.ButtonFactory;

@Singleton
public class StockpileSelectionGuiView implements GuiView, DisplaysText {

	private final int ITEMS_PER_ROW = 5;
	private final Skin skin;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final TooltipFactory tooltipFactory;
	private final StockpileGroupDictionary stockpileGroupDictionary;
	private final ButtonFactory buttonFactory;

	private Button backButton;
	private Table mainTable;
	private Table buttonsTable;

	@Inject
	public StockpileSelectionGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
									 StockpileGroupDictionary stockpileGroupDictionary, I18nTranslator i18nTranslator,
									 TooltipFactory tooltipFactory, ButtonFactory buttonFactory) {
		skin = guiSkinRepository.getMainGameSkin();
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.tooltipFactory = tooltipFactory;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.buttonFactory = buttonFactory;
	}

	@Override
	public void rebuildUI() {
		backButton = buttonFactory.buildDrawableButton("btn_back", "GUI.BACK_LABEL", () -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getParentViewName());
		});

		mainTable = new Table();
		mainTable.setTouchable(Touchable.enabled);
		mainTable.setBackground(skin.get("asset_dwarf_select_bg_patch", TenPatchDrawable.class));
		mainTable.pad(20);

		Container<Label> headerContainer = new Container<>();
		headerContainer.setBackground(skin.getDrawable("asset_bg_ribbon_title"));
		Label headerLabel = new Label(i18nTranslator.getTranslatedString("ROOMS.STOCKPILES").toString(), skin.get("title-header", Label.LabelStyle.class));
		headerContainer.setActor(headerLabel);
		headerContainer.center();

		mainTable.add(headerContainer).center().expandY().padBottom(20).row();

		buttonsTable = new Table();

		int rowCursor = 0;
		for (StockpileGroup stockpileGroup : stockpileGroupDictionary.getAll(i18nTranslator)) {

			Drawable drawable;
			try {
				drawable = skin.getDrawable(stockpileGroup.getDrawableName());
			} catch (GdxRuntimeException e) {
				// To handle when drawable not found
				drawable = skin.getDrawable("placeholder");
			}
			Button stockpileGroupButton = new Button(drawable);
			buttonFactory.attachClickCursor(stockpileGroupButton, GameCursor.SELECT);
			stockpileGroupButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.GUI_STOCKPILE_GROUP_SELECTED, stockpileGroup);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_ROOM);
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ROOM_EDITING);
				}
			});
			tooltipFactory.simpleTooltip(stockpileGroupButton, stockpileGroup.getI18nKey(), TooltipLocationHint.ABOVE);

			Container<Button> buttonContainer = new Container<>();
			buttonContainer.pad(10);
			buttonContainer.setBackground(skin.getDrawable("stockpile_bg"));
			buttonContainer.setActor(stockpileGroupButton);

			buttonsTable.add(buttonContainer);

			rowCursor++;
			if (rowCursor % ITEMS_PER_ROW == 0) {
				buttonsTable.row();
			}
		}

		mainTable.add(buttonsTable).padLeft(30).padRight(30).padBottom(50).center().row();
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.STOCKPILE_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.ROOM_SELECTION;
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
