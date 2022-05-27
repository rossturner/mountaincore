package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class ReplaceFloorMessage {

	public final GridPoint2 targetLocation;
	public final FloorType newFloorType;
	public final GameMaterial newMaterial;

	public ReplaceFloorMessage(GridPoint2 targetLocation, FloorType newFloorType, GameMaterial newMaterial) {
		this.targetLocation = targetLocation;
		this.newFloorType = newFloorType;
		this.newMaterial = newMaterial;
	}
}
