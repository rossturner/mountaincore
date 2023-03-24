package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

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
