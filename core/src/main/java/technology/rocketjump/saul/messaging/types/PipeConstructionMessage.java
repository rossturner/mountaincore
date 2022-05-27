package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class PipeConstructionMessage {
	public final GridPoint2 tilePosition;
	public final GameMaterial material;

	public PipeConstructionMessage(GridPoint2 tilePosition, GameMaterial material) {
		this.tilePosition = tilePosition;
		this.material = material;
	}
}
