package technology.rocketjump.saul.ui.actions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.views.GuiViewName;

public class RoomSelectedAction extends SwitchGuiViewAction {

	private final RoomType selectedRoomType;
	private StockpileGroup stockpileGroup;

	public RoomSelectedAction(RoomType selectedRoomType, MessageDispatcher messageDispatcher) {
		super(GuiViewName.ROOM_SIZING, messageDispatcher);
		this.selectedRoomType = selectedRoomType;
	}

	@Override
	public void onClick() {
		if (stockpileGroup != null) {
			messageDispatcher.dispatchMessage(MessageType.GUI_STOCKPILE_GROUP_SELECTED, stockpileGroup);
		}
		if (selectedRoomType != null) {
			messageDispatcher.dispatchMessage(MessageType.GUI_ROOM_TYPE_SELECTED, selectedRoomType);
			messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.PLACE_ROOM);
		}
		super.onClick();
	}

	public void setStockpileGroup(StockpileGroup stockpileGroup) {
		this.stockpileGroup = stockpileGroup;
	}

	public StockpileGroup getStockpileGroup() {
		return stockpileGroup;
	}
}
