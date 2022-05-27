package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class PlantCreationRequestMessage {

	private final GameMaterial seedMaterial;
	private final EntityCreatedCallback callback;

	public PlantCreationRequestMessage(GameMaterial seedMaterial, EntityCreatedCallback callback) {
		this.seedMaterial = seedMaterial;
		this.callback = callback;
	}

	public GameMaterial getSeedMaterial() {
		return seedMaterial;
	}

	public EntityCreatedCallback getCallback() {
		return callback;
	}
}
