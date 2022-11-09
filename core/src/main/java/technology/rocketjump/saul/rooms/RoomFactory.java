package technology.rocketjump.saul.rooms;

import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.entities.tags.TagProcessor;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.TileNeighbours;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.rooms.components.RoomComponent;
import technology.rocketjump.saul.rooms.components.StockpileRoomComponent;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;

import java.util.Iterator;
import java.util.Map;

@Singleton
public class RoomFactory implements GameContextAware, DisplaysText {

	private final TagProcessor tagProcessor;
	private final RoomStore roomStore;
	private final I18nTranslator i18nTranslator;
	private GameContext gameContext;

	@Inject
	public RoomFactory(TagProcessor tagProcessor, RoomStore roomStore, I18nTranslator i18nTranslator) {
		this.tagProcessor = tagProcessor;
		this.roomStore = roomStore;
		this.i18nTranslator = i18nTranslator;
	}


	public Room create(RoomType roomType) {
		Room room = new Room(roomType);
		I18nText translatedName = i18nTranslator.getTranslatedString(roomType.getI18nKey());
		tagProcessor.apply(roomType.getProcessedTags(), room);
		room.setRoomName(generateSequentialName(translatedName, 1));
		if (!roomType.getRoomName().equals("VIRTUAL_PLACING_ROOM")) {
			roomStore.add(room);
		}
		return room;
	}

	public Room createBasedOn(Room originalRoom) {
		Room newRoom = create(originalRoom.getRoomType());
		for (RoomComponent originalComponent : originalRoom.getAllComponents()) {
			RoomComponent cloned = originalComponent.clone(newRoom);
			newRoom.addComponent(cloned);
			if (cloned instanceof StockpileRoomComponent) {
				Iterator<StockpileGroup> groupIterator = ((StockpileRoomComponent) cloned).getEnabledGroups().iterator();
				if (groupIterator.hasNext()) {
					String originalName = newRoom.getRoomName();
					newRoom.setRoomName(generateSequentialName(getStockpileRoomName(newRoom.getRoomType(), groupIterator.next()), 1));
					roomStore.nameChanged(newRoom, originalName);
				}
			}
		}
		return newRoom;
	}

	public Room create(RoomType roomType, Map<GridPoint2, RoomTile> roomTilesToPlace) {
		Room room = create(roomType);

		RoomTile firstRoomTile = roomTilesToPlace.values().iterator().next();
		roomTilesToPlace.remove(firstRoomTile.getTilePosition());
		addTilesToRoom(firstRoomTile, roomTilesToPlace, room);
		room.updateLayout(gameContext.getAreaMap());
		return room;
	}

	private void addTilesToRoom(RoomTile currentRoomTile, Map<GridPoint2, RoomTile> remainingRoomTiles, Room newRoom) {
		currentRoomTile.setRoom(newRoom);
		MapTile mapTile = gameContext.getAreaMap().getTile(currentRoomTile.getTilePosition());
		newRoom.addTile(currentRoomTile);
		mapTile.setRoomTile(currentRoomTile);

		TileNeighbours orthogonalNeighbours = gameContext.getAreaMap().getOrthogonalNeighbours(mapTile.getTileX(), mapTile.getTileY());
		for (MapTile neighbourTile : orthogonalNeighbours.values()) {
			if (remainingRoomTiles.containsKey(neighbourTile.getTilePosition())) {
				RoomTile neighbourRoomTile = remainingRoomTiles.remove(neighbourTile.getTilePosition());
				addTilesToRoom(neighbourRoomTile, remainingRoomTiles, newRoom);
			}
		}
	}

	@Override
	public void rebuildUI() {
		for (Room room : roomStore.getAll()) {
			if (!room.isNameChangedByPlayer()) {
				I18nText roomNameText = i18nTranslator.getTranslatedString(room.getRoomType().getI18nKey());
				StockpileRoomComponent stockpileRoomComponent = room.getComponent(StockpileRoomComponent.class);
				if (stockpileRoomComponent != null) {
					if (!stockpileRoomComponent.getEnabledGroups().isEmpty()) {
						StockpileGroup group = stockpileRoomComponent.getEnabledGroups().iterator().next();
						roomNameText = getStockpileRoomName(room.getRoomType(), group);
					}
				}
				String originalName = room.getRoomName();
				room.setRoomName(generateSequentialName(roomNameText, 1));
				roomStore.nameChanged(room, originalName);
			}
		}
	}

	public void updateRoomNameForStockpileGroup(Room room, StockpileGroup stockpileGroup) {
		String originalName = room.getRoomName();
		room.setRoomName(generateSequentialName(getStockpileRoomName(room.getRoomType(), stockpileGroup), 1));
		roomStore.nameChanged(room, originalName);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		rebuildUI();
	}

	@Override
	public void clearContextRelatedState() {

	}

	public I18nText getStockpileRoomName(RoomType roomType, StockpileGroup stockpileGroup) {
		return i18nTranslator.applyReplacements(i18nTranslator.getWord("ROOMS.STOCKPILE_WITH_GROUP_NAME"), Map.of(
				"roomTypeName", i18nTranslator.getTranslatedString(roomType.getI18nKey()),
				"stockpileGroupName", stockpileGroup != null ? i18nTranslator.getTranslatedString(stockpileGroup.getI18nKey()).toLowerCase() : I18nWord.BLANK
		), Gender.ANY);
	}

	private String generateSequentialName(I18nText roomNameText, int counter) {
		String sequentialName = i18nTranslator.applyReplacements(i18nTranslator.getWord("ROOMS.ITERATIVE_NAMED_ROOM"),
				Map.of(
						"roomName", roomNameText,
						"number", new I18nWord(String.valueOf(counter))
				), Gender.ANY).toString();

		if (roomStore.getByName(sequentialName) == null) {
			return sequentialName;
		} else {
			return generateSequentialName(roomNameText, counter + 1);
		}
	}

}
