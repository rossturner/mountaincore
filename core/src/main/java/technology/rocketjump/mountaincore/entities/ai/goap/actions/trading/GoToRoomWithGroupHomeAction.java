package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.rooms.RoomTile;

import java.util.ArrayList;
import java.util.Collections;

public class GoToRoomWithGroupHomeAction extends GoToLocationAction {

	public GoToRoomWithGroupHomeAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			CreatureGroup creatureGroup = creatureBehaviour.getCreatureGroup();
			if (creatureGroup != null && creatureGroup.getHomeLocation() != null) {
				MapTile homeTile = gameContext.getAreaMap().getTile(creatureGroup.getHomeLocation());
				if (homeTile.getRoomTile() != null) {
					ArrayList<RoomTile> roomTiles = new ArrayList<>(homeTile.getRoomTile().getRoom().getRoomTiles().values());
					Collections.shuffle(roomTiles, gameContext.getRandom());

					for (RoomTile roomTile : roomTiles) {
						if (roomTile.getTile().isNavigable(parent.parentEntity)) {
							return roomTile.getTile().getWorldPositionOfCenter();
						}
					}
				}
			}
		}
		return null;
	}

}
