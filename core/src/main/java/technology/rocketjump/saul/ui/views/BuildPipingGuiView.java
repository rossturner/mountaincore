package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameViewMode;
import technology.rocketjump.saul.ui.actions.SetInteractionMode;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ButtonStyle;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;
import technology.rocketjump.saul.ui.widgets.IconButton;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class BuildPipingGuiView implements GuiView {

	private final SettlementItemTracker settlementItemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;


	private final List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public BuildPipingGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							  IconButtonFactory iconButtonFactory, SettlementItemTracker settlementItemTracker,
							  I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory) {
		this.messageDispatcher = messageDispatcher;
		this.settlementItemTracker = settlementItemTracker;
		this.i18nTranslator = i18nTranslator;

		Skin uiSkin = guiSkinRepository.getDefault();

		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.OLD_BUILD_MENU);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
		});
		iconButtons.add(back);

		// add roofing
		IconButton addPiping = iconButtonFactory.create("GUI.ROOFING.ADD", "pipes", HexColors.POSITIVE_COLOR, ButtonStyle.DEFAULT);
		addPiping.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_PIPING, messageDispatcher));
		iconButtons.add(addPiping);

		// cancel roofing
		IconButton cancelRoofing = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.DEFAULT);
		cancelRoofing.setAction(new SetInteractionMode(GameInteractionMode.CANCEL_PIPING, messageDispatcher));
		iconButtons.add(cancelRoofing);

		// deconstruct roofing
		IconButton deconstructRoofing = iconButtonFactory.create("GUI.DECONSTRUCT_LABEL", "demolish", HexColors.get("#d1752e"), ButtonStyle.DEFAULT);
		deconstructRoofing.setAction(new SetInteractionMode(GameInteractionMode.DECONSTRUCT_PIPING, messageDispatcher));
		iconButtons.add(deconstructRoofing);

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_PIPING;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.OLD_BUILD_MENU;
	}

	@Override
	public void populate(Table containerTable) {
		for (IconButton iconButton : iconButtons) {
			containerTable.add(iconButton).pad(5);
		}
	}

	@Override
	public void update() {
		// Doesn't yet need to update every second
	}

	@Override
	public void onHide() {
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
	}
}
