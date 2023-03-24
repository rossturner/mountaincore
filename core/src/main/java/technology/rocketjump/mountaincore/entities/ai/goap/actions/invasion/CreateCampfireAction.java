package technology.rocketjump.mountaincore.entities.ai.goap.actions.invasion;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.FurnitureAttributesCreationRequestMessage;
import technology.rocketjump.mountaincore.messaging.types.FurnitureCreationRequestMessage;
import technology.rocketjump.mountaincore.messaging.types.LookupFurnitureMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.Map;
import java.util.Set;

public class CreateCampfireAction extends Action {

	// FIXME this shouldn't be hard-coded
	public static final String CAMPFIRE_FURNITURE_TYPE_NAME = "INVADER_CAMPFIRE";

	public CreateCampfireAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		MapTile targetTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (targetTile != null && targetTile.isEmpty()) {
			parent.messageDispatcher.dispatchMessage(MessageType.LOOKUP_FURNITURE_TYPE, new LookupFurnitureMessage(CAMPFIRE_FURNITURE_TYPE_NAME, furnitureType -> {
				if (furnitureType != null) {
					GameMaterialType materialType = furnitureType.getRequirements().keySet().iterator().next();
					parent.messageDispatcher.dispatchMessage(MessageType.FURNITURE_ATTRIBUTES_CREATION_REQUEST, new FurnitureAttributesCreationRequestMessage(
							furnitureType, materialType, furnitureAttributes -> {

						parent.messageDispatcher.dispatchMessage(MessageType.FURNITURE_CREATION_REQUEST, new FurnitureCreationRequestMessage(
								furnitureAttributes, Map.of(), targetTile.getTilePosition(), Set.of(targetTile.getTilePosition()), furnitureEntity -> {
							FactionComponent factionComponent = furnitureEntity.getOrCreateComponent(FactionComponent.class);
							factionComponent.init(furnitureEntity, parent.messageDispatcher, gameContext);
							factionComponent.setFaction(parent.parentEntity.getOrCreateComponent(FactionComponent.class).getFaction());
							completionType = CompletionType.SUCCESS;
						}));
					}));

				}
			}));
		}


		if (completionType == null) {
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
	}
}
