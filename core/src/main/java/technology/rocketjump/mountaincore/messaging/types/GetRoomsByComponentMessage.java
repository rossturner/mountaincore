package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.RoomComponent;

import java.util.List;
import java.util.function.Consumer;

public record GetRoomsByComponentMessage(Class<? extends RoomComponent> type, Consumer<List<Room>> callback) {
}
