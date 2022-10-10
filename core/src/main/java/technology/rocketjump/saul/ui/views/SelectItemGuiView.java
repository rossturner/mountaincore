package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.combat.DefenseType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.PopulateSelectItemViewMessage;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nTextWidget;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.ImageButton;
import technology.rocketjump.saul.ui.widgets.ImageButtonFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class SelectItemGuiView implements GuiView {

	private final int ITEMS_PER_ROW = 5;
	private final Skin uiSkin;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final I18nWidgetFactory i18nWidgetFactory;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final SettlementItemTracker settlementItemTracker;
	private final ItemTypeDictionary itemTypeDictionary;
	private final ImageButtonFactory imageButtonFactory;
	private final Table selectNoneTable;
	private final ImageButton selectNoneButton;

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
							 SettlementItemTracker settlementItemTracker, ItemTypeDictionary itemTypeDictionary, ImageButtonFactory imageButtonFactory) {
		this.settlementItemTracker = settlementItemTracker;
		this.i18nWidgetFactory = i18nWidgetFactory;
		this.messageDispatcher = messageDispatcher;

		this.uiSkin = guiSkinRepository.getDefault();
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.i18nTranslator = i18nTranslator;
		this.itemTypeDictionary = itemTypeDictionary;
		this.imageButtonFactory = imageButtonFactory;

		viewTable = new Table(uiSkin);
		viewTable.background("default-rect");

		headingLabel = i18nWidgetFactory.createLabel("GUI.SELECT_WEAPON.LABEL");
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

		selectNoneTable = new Table(uiSkin);
		selectNoneButton = imageButtonFactory.getOrCreate("square");
		selectNoneTable.add(selectNoneButton).pad(5).row();
		selectNoneTable.add(i18nWidgetFactory.createLabel("WEAPON.NONE")).pad(2).row();
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(viewTable);
	}

	public void prepopulate(PopulateSelectItemViewMessage message) {
		itemsTable.clear();

		int numAdded = 0;

		selectNoneButton.setAction(() -> message.callback.accept(null));
		itemsTable.add(selectNoneTable);
		numAdded++;

		for (ItemType itemType : itemTypeDictionary.getAll()) {
			if (includedItemType(itemType, message)) {
				// Going to assume that equippable items are always not stackable
				for (Entity itemEntity : settlementItemTracker.getItemsByType(itemType, true)) {
					itemsTable.add(buildItemButton(itemEntity, message.callback));
					numAdded++;

					if (numAdded % ITEMS_PER_ROW == 0) {
						itemsTable.row();
					}
				}
			}
		}
	}

	private Table buildItemButton(Entity itemEntity, Consumer<Entity> callback) {
		Table innerTable = new Table(uiSkin);
		ImageButton imageButton = imageButtonFactory.getOrCreate(itemEntity);
		imageButton.setAction(() -> callback.accept(itemEntity));
		innerTable.add(imageButton).pad(5).row();
		innerTable.add(new I18nTextWidget(i18nTranslator.getDescription(itemEntity), uiSkin, messageDispatcher)).pad(2).row();
		return innerTable;
	}

	private boolean includedItemType(ItemType itemType, PopulateSelectItemViewMessage message) {
		return switch (message.itemSelectionCategory) {
			case WEAPON -> itemType.getWeaponInfo() != null;
			case SHIELD -> itemType.getDefenseInfo() != null && itemType.getDefenseInfo().getType().equals(DefenseType.SHIELD);
			case ARMOR -> itemType.getDefenseInfo() != null && itemType.getDefenseInfo().getType().equals(DefenseType.ARMOR) && itemType.getDefenseInfo().canBeEquippedBy(message.requestingEntity);
		};
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
