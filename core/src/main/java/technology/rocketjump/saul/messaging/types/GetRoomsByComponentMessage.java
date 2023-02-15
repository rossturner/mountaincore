package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.RoomComponent;

import java.util.List;
import java.util.function.Consumer;

public record GetRoomsByComponentMessage(Class<? extends RoomComponent> type, Consumer<List<Room>> callback) {
}
