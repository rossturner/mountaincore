package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;
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

import static technology.rocketjump.saul.ui.GameInteractionMode.DESIGNATE_MECHANISMS;

@Singleton
public class BuildMechanismsGuiView implements GuiView {

	private final SettlementItemTracker settlementItemTracker;
	private final I18nTranslator i18nTranslator;
	private final MessageDispatcher messageDispatcher;
	private final GameInteractionStateContainer interactionStateContainer;


	private final List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public BuildMechanismsGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
								  IconButtonFactory iconButtonFactory, SettlementItemTracker settlementItemTracker,
								  I18nTranslator i18nTranslator, I18nWidgetFactory i18NWidgetFactory,
								  GameInteractionStateContainer interactionStateContainer, MechanismTypeDictionary mechanismTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.settlementItemTracker = settlementItemTracker;
		this.i18nTranslator = i18nTranslator;
		this.interactionStateContainer = interactionStateContainer;

		MechanismType gearMechanismType = mechanismTypeDictionary.getByName("Gear");
		MechanismType horizontalShaftMechanismType = mechanismTypeDictionary.getByName("Shaft_Horizontal");
		MechanismType verticalShaftMechanismType = mechanismTypeDictionary.getByName("Shaft_Vertical");

		Skin uiSkin = guiSkinRepository.getDefault();

		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.OLD_BUILD_MENU);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
		});
		iconButtons.add(back);

		IconButton addGears = iconButtonFactory.create("PRODUCT.STONE.GEAR", "gears", HexColors.get("#DDDDDE"), ButtonStyle.DEFAULT);
		addGears.setAction(() -> {
			interactionStateContainer.setMechanismTypeToPlace(gearMechanismType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_MECHANISMS);
		});
		iconButtons.add(addGears);

		IconButton addHorizontalShafts = iconButtonFactory.create("GUI.MECHANISM.SHAFT.HORIZONTAL", "straight-line-horiz", HexColors.get("#a1511b"), ButtonStyle.DEFAULT);
		addHorizontalShafts.setAction(() -> {
			interactionStateContainer.setMechanismTypeToPlace(horizontalShaftMechanismType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_MECHANISMS);
		});
		iconButtons.add(addHorizontalShafts);

		IconButton addVerticalShafts = iconButtonFactory.create("GUI.MECHANISM.SHAFT.VERTICAL", "straight-line", HexColors.get("#a1511b"), ButtonStyle.DEFAULT);
		addVerticalShafts.setAction(() -> {
			interactionStateContainer.setMechanismTypeToPlace(verticalShaftMechanismType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, DESIGNATE_MECHANISMS);
		});
		iconButtons.add(addVerticalShafts);

		IconButton cancel = iconButtonFactory.create("GUI.CANCEL_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.DEFAULT);
		cancel.setAction(new SetInteractionMode(GameInteractionMode.CANCEL_MECHANISMS, messageDispatcher));
		iconButtons.add(cancel);

		IconButton deconstruct = iconButtonFactory.create("GUI.DECONSTRUCT_LABEL", "demolish", HexColors.get("#d1752e"), ButtonStyle.DEFAULT);
		deconstruct.setAction(new SetInteractionMode(GameInteractionMode.DECONSTRUCT_MECHANISMS, messageDispatcher));
		iconButtons.add(deconstruct);
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.BUILD_MECHANISMS;
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
