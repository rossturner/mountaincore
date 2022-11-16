package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.rooms.Bridge;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
import technology.rocketjump.saul.ui.Selectable;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.*;

import static technology.rocketjump.saul.ui.Selectable.SelectableType.BRIDGE;

@Singleton
public class BridgeSelectedGuiView implements GuiView, GameContextAware {

	private final Skin uiSkin;
	private final I18nTranslator i18nTranslator;
	private final GameInteractionStateContainer gameInteractionStateContainer;
	private final IconButton removeButton;
	private final MessageDispatcher messageDispatcher;
	private Table outerTable;
	private Table descriptionTable;
	private Label beingDeconstructedLabel;

	@Inject
	public BridgeSelectedGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator,
								 GameInteractionStateContainer gameInteractionStateContainer, IconButtonFactory iconButtonFactory,
								 I18nWidgetFactory i18nWidgetFactory) {
		uiSkin = guiSkinRepository.getDefault();
		this.i18nTranslator = i18nTranslator;
		this.gameInteractionStateContainer = gameInteractionStateContainer;
		this.messageDispatcher = messageDispatcher;

		outerTable = new Table(uiSkin);
		outerTable.background("default-rect");
		outerTable.pad(10);

		descriptionTable = new Table(uiSkin);
		descriptionTable.pad(10);

		removeButton = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.SMALL);
		removeButton.setAction(() -> {
			Selectable selectable = gameInteractionStateContainer.getSelectable();
			if (selectable != null && selectable.type.equals(BRIDGE)) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_BRIDGE_REMOVAL, selectable.getBridge());
				doUpdate();
			}
		});

		beingDeconstructedLabel = i18nWidgetFactory.createLabel("GUI.FURNITURE_BEING_REMOVED");

	}

	@Override
	public void populate(Table containerTable) {
		update();

		containerTable.clear();
		containerTable.add(outerTable);
	}

	private Selectable currentSelectable;

	@Override
	public void update() {
		// Don't want to always update as this will lose focus on selectboxes
		Selectable selectable = gameInteractionStateContainer.getSelectable();

		if ((selectable != null && currentSelectable == null) ||
				(selectable != null && !selectable.equals(currentSelectable))) {
			currentSelectable = selectable;
			doUpdate();
		}
	}

	private void doUpdate() {
		outerTable.clear();

		descriptionTable.clear();
		if (currentSelectable != null && currentSelectable.type.equals(BRIDGE)) {
			Bridge bridge = currentSelectable.getBridge();

			descriptionTable.add(new I18nTextWidget(i18nTranslator.getDescription(bridge), uiSkin, messageDispatcher)).left().row();

			outerTable.add(descriptionTable).left();
			if (bridge.isBeingDeconstructed()) {
				outerTable.add(beingDeconstructedLabel).right().pad(4);
			} else {
				outerTable.add(removeButton).right().pad(4);
			}
		}
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BRIDGE_SELECTED;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
	}

	@Override
	public void onContextChange(GameContext gameContext) {

	}

	@Override
	public void clearContextRelatedState() {
		currentSelectable = null;
	}

}
