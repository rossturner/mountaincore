package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.GetRoomsByComponentMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.components.behaviour.TradeDepotBehaviour;

import java.util.ArrayList;
import java.util.Collections;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class MoveGroupHomeToTradeDepotAction extends Action {

	public MoveGroupHomeToTradeDepotAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			CreatureGroup creatureGroup = creatureBehaviour.getCreatureGroup();
			if (creatureGroup != null) {
				if (creatureGroup.getHomeLocation() == null) {
					return true;
				}
				RoomTile roomTile = gameContext.getAreaMap().getTile(creatureGroup.getHomeLocation()).getRoomTile();
				if (roomTile == null) {
					return true;
				}
				if (roomTile.getRoom().getComponent(TradeDepotBehaviour.class) == null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (isApplicable(gameContext) && parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			CreatureGroup creatureGroup = creatureBehaviour.getCreatureGroup();

			parent.messageDispatcher.dispatchMessage(MessageType.GET_ROOMS_BY_COMPONENT,
					new GetRoomsByComponentMessage(TradeDepotBehaviour.class, tradeDepots -> {
				if (!tradeDepots.isEmpty()) {
					Room tradeDepot = tradeDepots.get(gameContext.getRandom().nextInt(tradeDepots.size()));

					ArrayList<RoomTile> tiles = new ArrayList<>(tradeDepot.getRoomTiles().values());
					Collections.shuffle(tiles, gameContext.getRandom());

					for (RoomTile tile : tiles) {
						if (tile.getTile().isNavigable(parent.parentEntity)) {
							creatureGroup.setHomeLocation(tile.getTile().getTilePosition());
							completionType = SUCCESS;
							break;
						}
					}
				}

			}));
		}

		if (completionType == null) {
			completionType = FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
