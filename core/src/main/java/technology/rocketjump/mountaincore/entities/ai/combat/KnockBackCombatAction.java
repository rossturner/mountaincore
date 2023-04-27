package technology.rocketjump.mountaincore.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.components.creature.CombatStateComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.mountaincore.misc.VectorUtils.toVector;

// Extends DefensiveCombatAction to recover defense pool if it was already defending
public class KnockBackCombatAction extends DefensiveCombatAction {

	private Vector2 startLocation;
	private Vector2 targetLocation;

	public KnockBackCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		super.update(deltaTime, gameContext, messageDispatcher);
		TiledMap areaMap = gameContext.getAreaMap();
		MapTile startTile = areaMap.getTile(startLocation);
		MapTile targetTile = areaMap.getTile(targetLocation);
		if (startTile.getRegionId() != targetTile.getRegionId()) {
			completed = true;
			return;
		}

		parentEntity.getBehaviourComponent().getSteeringComponent().destinationReached();
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		combatStateComponent.setHeldLocation(toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));

		// Don't steer, just change location so we don't turn/face away
		Vector2 currentLocation = parentEntity.getLocationComponent().getWorldOrParentPosition();
		Vector2 moveThisFrame = targetLocation.cpy().sub(currentLocation).nor().scl(parentEntity.getLocationComponent().getMaxLinearSpeed()).scl(deltaTime);

		if (moveThisFrame.len2() > currentLocation.dst2(targetLocation)) {
			// Going to move past target
			parentEntity.getLocationComponent().setWorldPosition(targetLocation, false);
			completed = true;
		} else {
			parentEntity.getLocationComponent().setWorldPosition(currentLocation.cpy().add(moveThisFrame), false);
		}
	}


	public Vector2 getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Vector2 startLocation) {
		this.startLocation = startLocation;
	}

	public Vector2 getTargetLocation() {
		return targetLocation;
	}

	public void setTargetLocation(GridPoint2 targetLocation) {
		this.targetLocation = toVector(targetLocation);
	}


	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("startLocation", JSONUtils.toJSON(startLocation));
		asJson.put("targetLocation", JSONUtils.toJSON(targetLocation));
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.startLocation = JSONUtils.vector2(asJson.getJSONObject("startLocation"));
		this.targetLocation = JSONUtils.vector2(asJson.getJSONObject("targetLocation"));
	}
}
