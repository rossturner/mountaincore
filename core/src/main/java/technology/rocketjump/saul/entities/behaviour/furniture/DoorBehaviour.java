package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.assets.entities.furniture.model.DoorState;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.assets.entities.furniture.model.DoorState.*;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

public class DoorBehaviour implements BehaviourComponent {

	public static final float ANIMATION_TIME = 0.4f;

	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private GameContext gameContext;

	private float currentStateElapsedTime = 0;
	private boolean doorOpenRequested;
	private DoorState state = DoorState.CLOSED;
	private SoundAsset openSound;
	private SoundAsset closeSound;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;
	}

	@Override
	public DoorBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		DoorBehaviour cloned = new DoorBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		cloned.currentStateElapsedTime = this.currentStateElapsedTime;
		cloned.doorOpenRequested = this.doorOpenRequested;
		cloned.state = this.state;
		return cloned;
	}

	public DoorState getState() {
		return state;
	}

	public void setState(DoorState state) {
		this.state = state;
	}

	public void doorOpenRequested() {
		if (state.equals(CLOSED) || state.equals(CLOSING)) {
			doorOpenRequested = true;
		}
	}

	public void setSoundAssets(SoundAsset openSound, SoundAsset closeSound) {
		this.openSound = openSound;
		this.closeSound = closeSound;
	}

	@Override
	public void update(float deltaTime) {
		currentStateElapsedTime += deltaTime;

		PhysicalEntityComponent physicalEntityComponent = parentEntity.getPhysicalEntityComponent();
		float currentAnimationProgress = physicalEntityComponent.getAnimationProgress();

		MapTile doorTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldPosition());
		boolean otherEntityInTile = doorTile.getEntities().size() > 1;

		if (doorOpenRequested) {
			switchState(OPENING);
			doorOpenRequested = false;
		} else {
			switch (state) {
				case CLOSED:
					break;
				case OPENING:
					currentAnimationProgress += (deltaTime / ANIMATION_TIME);
					if (currentAnimationProgress >= 1f) {
						currentAnimationProgress = 1f;
						switchState(OPEN);
					}
					physicalEntityComponent.setAnimationProgress(currentAnimationProgress);
					break;
				case OPEN:
					// Check for entities in doorway, if so, start closing
					if (otherEntityInTile) {
						currentStateElapsedTime = 0f;
					}

					if (currentStateElapsedTime > 1) {
						switchState(CLOSING);
					}
					break;
				case CLOSING:
					currentAnimationProgress -= (deltaTime / ANIMATION_TIME);

					if (otherEntityInTile) {
						switchState(OPENING);
					} else if (currentAnimationProgress <= 0f) {
						currentAnimationProgress = 0f;
						switchState(CLOSED);
					}
					physicalEntityComponent.setAnimationProgress(currentAnimationProgress);
					break;
			}
		}

	}

	@Override
	public void updateWhenPaused() {

	}

	private void switchState(DoorState newState) {
		DoorState previousState = this.state;
		if (previousState.equals(newState)) {
			return;
		} else {
			currentStateElapsedTime = 0;
		}
		this.state = newState;

		if (previousState.equals(CLOSED) && newState.equals(OPENING)) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(openSound, parentEntity.getId(),
					parentEntity.getLocationComponent().getWorldOrParentPosition(), null));
		} else if (previousState.equals(OPEN) && newState.equals(CLOSING)) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(closeSound, parentEntity.getId(),
					parentEntity.getLocationComponent().getWorldOrParentPosition(), null));
		}

		if (previousState.equals(CLOSED) || newState.equals(CLOSED)) {
			messageDispatcher.dispatchMessage(MessageType.DOOR_OPENED_OR_CLOSED, toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
		}
	}


	@Override
	public void infrequentUpdate(GameContext gameContext) {
		// Do nothing, does not update infrequently
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
		if (currentStateElapsedTime != 0f) {
			asJson.put("elapsedTime", currentStateElapsedTime);
		}
		if (doorOpenRequested) {
			asJson.put("openRequested", true);
		}
		if (!state.equals(CLOSED)) {
			asJson.put("state", state.name());
		}

		if (openSound != null) {
			asJson.put("openSound", openSound.getName());
		}
		if (closeSound != null) {
			asJson.put("closeSound", closeSound.getName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.currentStateElapsedTime = asJson.getFloatValue("elapsedTime");
		this.doorOpenRequested = asJson.getBooleanValue("openRequested");
		this.state = EnumParser.getEnumValue(asJson, "state", DoorState.class, DoorState.CLOSED);

		String openSoundName = asJson.getString("openSound");
		if (openSoundName != null) {
			this.openSound = relatedStores.soundAssetDictionary.getByName(openSoundName);
			if (this.openSound == null) {
				throw new InvalidSaveException("Could not find sound by name " + openSoundName);
			}
		}
		String closeSoundName = asJson.getString("closeSound");
		if (closeSoundName != null) {
			this.closeSound = relatedStores.soundAssetDictionary.getByName(closeSoundName);
			if (this.closeSound == null) {
				throw new InvalidSaveException("Could not find sound by name " + closeSoundName);
			}
		}
	}
}
