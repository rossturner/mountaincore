package technology.rocketjump.saul.ui.views;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.ui.GameInteractionMode;

import java.util.EnumMap;
import java.util.Map;

@Singleton
public class GuiViewRepository implements Telegraph {

	private Map<GuiViewName, GuiView> byName = new EnumMap<>(GuiViewName.class);

	@Inject
	public GuiViewRepository(DefaultGuiView defaultGuiView, OrderSelectionGuiView orderSelectionGuiView, RoomSelectionGuiView roomSelectionGuiView,
							 RoomSizingGuiView roomSizingGuiView, FurnitureSelectionGuiView furnitureSelectionGuiView, EntitySelectedGuiView entitySelectedGuiView,
							 BuildMenuGuiView buildMenuGuiView, BuildFlooringGuiView buildFlooringGuiView,
							 BuildWallsGuiView buildWallsGuiView, ConstructionSelectedGuiView constructionSelectedGuiView,
							 BuildDoorsGuiView buildDoorsGuiView, DoorwaySelectedGuiView doorwaySelectedGuiView, TileSelectedGuiView tileSelectedGuiView,
							 BuildRoofingGuiView buildRoofingGuiView, BuildPipingGuiView buildPipingGuiView,
							 BuildMechanismsGuiView buildMechanismsGuiView,
							 RoomSelectedGuiView roomSelectedGuiView, StockpileSelectionGuiView stockpileSelectionGuiView,
							 ChangeProfessionGuiView changeProfessionGuiView, ChangeWeaponSelectionGuiView changeWeaponSelectionGuiView,
							 BuildBridgeGuiView buildBridgeGuiView,
							 BridgeSelectedGuiView bridgeSelectedGuiView, PrioritiesGuiView prioritiesGuiView,
							 SelectStartLocationGuiView selectStartLocationGuiView,
							 MessageDispatcher messageDispatcher) {
		this(messageDispatcher, defaultGuiView, orderSelectionGuiView, roomSelectionGuiView, roomSizingGuiView, constructionSelectedGuiView,
				furnitureSelectionGuiView, entitySelectedGuiView, buildMenuGuiView, buildFlooringGuiView, buildWallsGuiView,
				buildDoorsGuiView, doorwaySelectedGuiView,
				tileSelectedGuiView, buildRoofingGuiView, buildPipingGuiView, roomSelectedGuiView, stockpileSelectionGuiView,
				changeProfessionGuiView, changeWeaponSelectionGuiView, buildBridgeGuiView, buildMechanismsGuiView,
				bridgeSelectedGuiView, prioritiesGuiView, selectStartLocationGuiView);
	}

	public GuiViewRepository(MessageDispatcher messageDispatcher, GuiView... views) {
		for (GuiView view : views) {
			add(view);
		}

		messageDispatcher.addListener(this, MessageType.GUI_ROOM_TYPE_SELECTED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GUI_ROOM_TYPE_SELECTED: {
				RoomType selectedRoomType = (RoomType) msg.extraInfo;
				FurnitureSelectionGuiView furnitureSelectionView = (FurnitureSelectionGuiView) byName.get(GuiViewName.ROOM_FURNITURE_SELECTION);
				furnitureSelectionView.setCurrentRoomType(selectedRoomType);

				RoomSizingGuiView guiView = (RoomSizingGuiView) byName.get(GuiViewName.ROOM_SIZING);
				guiView.setCurrentRoomType(selectedRoomType);
				GameInteractionMode.PLACE_ROOM.setRoomType(selectedRoomType);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void add(GuiView view) {
		if (byName.containsKey(view.getName())) {
			throw new RuntimeException("Duplicate GuiView name " + view.getName().name() + " attempting to be added to " + this.getClass().getSimpleName());
		}
		byName.put(view.getName(), view);
	}

	public GuiView getByName(GuiViewName name) {
		return byName.get(name);
	}
}
