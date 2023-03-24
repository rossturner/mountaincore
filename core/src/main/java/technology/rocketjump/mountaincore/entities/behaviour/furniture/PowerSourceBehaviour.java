package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState;
import technology.rocketjump.mountaincore.mapping.tile.underground.UnderTile;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PowerSourceBehaviour extends FurnitureBehaviour implements Destructible, SelectableDescription {

	private Boolean isOutside;

	@Override
	public void update(float deltaTime) {
		if (isWorking(gameContext)) {
			PoweredFurnitureComponent poweredFurnitureComponent = parentEntity.getComponent(PoweredFurnitureComponent.class);
			if (poweredFurnitureComponent != null) {
				poweredFurnitureComponent.update(deltaTime, gameContext);
			}
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		this.isOutside = isOutside(gameContext);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		if (parentTile != null) {
			UnderTile underTile = parentTile.getUnderTile();
			if (underTile != null) {
				underTile.setPowerSource(false);
			}
		}
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (isWorking(gameContext)) {
			return Collections.emptyList();
		} else {
			return List.of(
					i18nTranslator.getTranslatedString("MECHANISM.FURNITURE.REQUIRES_OUTSIDE_PLACEMENT")
			);
		}
	}

	public boolean isWorking(GameContext gameContext) {
		if (isOutside == null) {
			this.isOutside = isOutside(gameContext);
		}
		return isOutside;
	}

	private boolean isOutside(GameContext gameContext) {
		Set<MapTile> locationsToCheck = new HashSet<>();
		GridPoint2 parentLocation = VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldPosition());
		locationsToCheck.add(gameContext.getAreaMap().getTile(parentLocation));
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		for (GridPoint2 extraTile : attributes.getCurrentLayout().getExtraTiles()) {
			MapTile tile = gameContext.getAreaMap().getTile(
					parentLocation.x + extraTile.x,
					parentLocation.y + extraTile.y
			);
			locationsToCheck.add(tile);
		}

		for (MapTile tile : locationsToCheck) {
			if (!tile.getRoof().getState().equals(TileRoofState.OPEN)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (isOutside) {
			asJson.put("isOutside", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.isOutside = asJson.getBooleanValue("isOutside");
	}
}
