package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.PopulateSelectItemViewMessage;
import technology.rocketjump.saul.settlement.ItemTracker;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.ImageButton;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class SelectItemGuiView implements GuiView {

	private final int ITEMS_PER_ROW = 5;
	private final Skin uiSkin;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final ItemTracker itemTracker;
	private final ItemTypeDictionary itemTypeDictionary;

	private Table viewTable;
	private Table itemsTable;
	private ScrollPane scrollPane;

	private final Label headingLabel;
	private final TextButton backButton;
	private final List<ImageButton> itemButtons = new ArrayList<>();


	@Inject
	public SelectItemGuiView(I18nWidgetFactory i18nWidgetFactory,
							 GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							 GameInteractionStateContainer gameInteractionStateContainer, I18nTranslator i18nTranslator,
							 ItemTracker itemTracker, ItemTypeDictionary itemTypeDictionary) {
		this.itemTracker = itemTracker;
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;

		this.uiSkin = guiSkinRepository.getDefault();
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.i18nTranslator = i18nTranslator;
		this.itemTypeDictionary = itemTypeDictionary;

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");

		headingLabel = i18nWidgetFactory.createLabel("GUI.CHANGE_PROFESSION_LABEL");
		viewTable.add(headingLabel).center();
		viewTable.row();

		itemsTable = new Table(uiSkin);

		scrollPane = new ScrollPane(itemsTable, uiSkin);
		scrollPane.setScrollingDisabled(true, false);
		scrollPane.setForceScroll(false, true);
		ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle(uiSkin.get(ScrollPane.ScrollPaneStyle.class));
		scrollPaneStyle.background = null;
		scrollPane.setStyle(scrollPaneStyle);
		scrollPane.setFadeScrollBars(false);

		viewTable.add(scrollPane);//.height(400);
		viewTable.row();

		backButton = i18nWidgetFactory.createTextButton("GUI.BACK_LABEL");
		backButton.addListener(new ClickListener() {
			@Override
			public void clicked (InputEvent event, float x, float y) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.ENTITY_SELECTED);
			}
		});
		viewTable.add(backButton).pad(10).left();
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(viewTable);
	}

	public void prepopulate(PopulateSelectItemViewMessage message) {
		itemsTable.clear();

		int numAdded = 0;

		for (ItemType itemType : itemTypeDictionary.getAll()) {
			if (includedItemType(itemType, message.itemSelectionCategory)) {

			}
		}



		for (ImageButton imageButton : itemButtons) {
			Table innerTable = new Table(uiSkin);
			innerTable.add(imageButton).pad(5).row();
//			innerTable.add(new Label(i18nTranslator.get, uiSkin)).row();
//			innerTable.add(i18nWidgetFactory.createLabel(imageButton1.getI18nKey()));

			itemsTable.add(innerTable).pad(3);
			numAdded++;

			if (numAdded % ITEMS_PER_ROW == 0) {
				itemsTable.row();
			}
		}



	}

	private boolean includedItemType(ItemType itemType, PopulateSelectItemViewMessage.ItemSelectionCategory selectionCategory) {

		return false;
	}

	@Override
	public void update() {

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.SELECT_ITEM;
	}

	@Override
	public GuiViewName getParentViewName() {
		// General cancel (right-click) de-selects current entity, so can't go back to entity selected view, just go back to default menu
		return GuiViewName.DEFAULT_MENU;
	}
}
