package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class MechanismConstructionMessage {

	public final GridPoint2 location;
	public final MechanismType mechanismType;
	public final GameMaterial material;

	public MechanismConstructionMessage(GridPoint2 location, MechanismType mechanismType, GameMaterial material) {
		this.location = location;
		this.mechanismType = mechanismType;
		this.material = material;
	}
}
