package technology.rocketjump.saul.entities.behaviour.plants;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesItem;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.TreeFallenMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class FallingTreeBehaviour implements BehaviourComponent {

	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;

	private boolean fallToWest;
	private float absoluteRotationAmount = 0f;

	public FallingTreeBehaviour() {

	}

	public FallingTreeBehaviour(boolean fallToWest) {
		this.fallToWest = fallToWest;
	}

	@Override
	public FallingTreeBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FallingTreeBehaviour cloned = new FallingTreeBehaviour(fallToWest);
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
	}

	private static final float ROTATION_DEGREES_PER_SECOND = 120f;

	@Override
	public void update(float deltaTime) {
		PlantEntityAttributes attributes = (PlantEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

		float rotationChange = 0.05f + (0.9f * (absoluteRotationAmount / 90));
		float extraRotation = deltaTime * rotationChange * ROTATION_DEGREES_PER_SECOND;

		absoluteRotationAmount += extraRotation;

		if (fallToWest) {
			parentEntity.getLocationComponent().setRotation(absoluteRotationAmount);
		} else {
			parentEntity.getLocationComponent().setRotation(-absoluteRotationAmount);
		}

		if (absoluteRotationAmount > 85f) {
			// Tree has collapsed

			PlantSpeciesGrowthStage currentGrowthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());

			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);

			Color leafColor = attributes.getColor(ColoringLayer.LEAF_COLOR);
			if (leafColor != null && leafColor.equals(Color.CLEAR)) {
				leafColor = null;
			}

			List<PlantSpeciesItem> harvestedItems = currentGrowthStage.getHarvestedItems();
			if (attributes.isBurned()) {
				harvestedItems = emptyList();
			}
			messageDispatcher.dispatchMessage(MessageType.TREE_FELLED, new TreeFallenMessage(
					parentEntity.getLocationComponent().getWorldPosition(), attributes.getColor(ColoringLayer.BRANCHES_COLOR),
					Optional.ofNullable(leafColor),
					fallToWest, harvestedItems));
		}

	}

	@Override
	public void updateWhenPaused() {
		
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		// Do nothing, is not infrequent updater
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return false;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (fallToWest) {
			asJson.put("fallToWest", true);
		}
		asJson.put("rotation", absoluteRotationAmount);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.fallToWest = asJson.getBooleanValue("fallToWest");
		this.absoluteRotationAmount = asJson.getFloatValue("rotation");
	}
}
