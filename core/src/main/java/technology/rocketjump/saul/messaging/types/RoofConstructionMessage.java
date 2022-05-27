package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class RoofConstructionMessage {
	public final GridPoint2 roofTileLocation;
	public final GameMaterial roofMaterial;

	public RoofConstructionMessage(GridPoint2 roofTileLocation, GameMaterial roofMaterial) {
		this.roofTileLocation = roofTileLocation;
		this.roofMaterial = roofMaterial;
	}
}
