package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

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
