package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class AddWallMessage {

	public final GridPoint2 location;
	public final GameMaterial material;
	public final WallType wallType;

	public AddWallMessage(GridPoint2 location, GameMaterial material, WallType wallType) {
		this.location = location;
		this.material = material;
		this.wallType = wallType;
	}
}
