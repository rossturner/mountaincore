package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.actions.SetInteractionMode;
import technology.rocketjump.saul.ui.actions.SwitchGuiViewAction;
import technology.rocketjump.saul.ui.widgets.ButtonStyle;
import technology.rocketjump.saul.ui.widgets.IconButton;
import technology.rocketjump.saul.ui.widgets.IconButtonFactory;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class OrderSelectionGuiView implements GuiView {

	private List<IconButton> iconButtons = new LinkedList<>();

	@Inject
	public OrderSelectionGuiView(IconButtonFactory iconButtonFactory, MessageDispatcher messageDispatcher) {

		IconButton back = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
		back.setAction(new SwitchGuiViewAction(GuiViewName.DEFAULT_MENU, messageDispatcher));
		iconButtons.add(back);

		IconButton mine = iconButtonFactory.create("GUI.ORDERS.MINE", "mining", HexColors.get("#97CFC7"), ButtonStyle.DEFAULT);
		mine.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_MINING, messageDispatcher));
		iconButtons.add(mine);

		IconButton chopTrees = iconButtonFactory.create("GUI.ORDERS.CHOP_WOOD", "logging", HexColors.get("#41AB44"), ButtonStyle.DEFAULT);
		chopTrees.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_CHOP_WOOD, messageDispatcher));
		iconButtons.add(chopTrees);

		IconButton harvest = iconButtonFactory.create("GUI.ORDERS.HARVEST_PLANTS", "sickle", HexColors.get("#e0dc6a"), ButtonStyle.DEFAULT);
		harvest.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_HARVEST_PLANTS, messageDispatcher));
		iconButtons.add(harvest);

		IconButton clearGround = iconButtonFactory.create("GUI.ORDERS.CLEAR_GROUND", "spade", HexColors.get("#e1a774"), ButtonStyle.DEFAULT);
		clearGround.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_CLEAR_GROUND, messageDispatcher));
		iconButtons.add(clearGround);

		IconButton digChannels = iconButtonFactory.create("GUI.ORDERS.DIG_CHANNELS", "trench", HexColors.get("#b56a28"), ButtonStyle.DEFAULT);
		digChannels.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_DIG_CHANNEL, messageDispatcher));
		iconButtons.add(digChannels);

		IconButton extinguishFlames = iconButtonFactory.create("GUI.ORDERS.EXTINGUISH_FLAMES", "water-splash", HexColors.get("#5ae9f0"), ButtonStyle.DEFAULT);
		extinguishFlames.setAction(new SetInteractionMode(GameInteractionMode.DESIGNATE_EXTINGUISH_FLAMES, messageDispatcher));
		iconButtons.add(extinguishFlames);

		IconButton removeDesignations = iconButtonFactory.create("GUI.REMOVE_LABEL", "cancel", HexColors.NEGATIVE_COLOR, ButtonStyle.DEFAULT);
		removeDesignations.setAction(new SetInteractionMode(GameInteractionMode.REMOVE_DESIGNATIONS, messageDispatcher));
		iconButtons.add(removeDesignations);

	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.ORDER_SELECTION;
	}

	@Override
	public GuiViewName getParentViewName() {
		return GuiViewName.DEFAULT_MENU;
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

}
