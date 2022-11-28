package technology.rocketjump.saul.mapping.model;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;

public class MechanismPlacement {

	public final GridPoint2 location;
	public MechanismType mechanismType;

	public MechanismPlacement(GridPoint2 location, MechanismType mechanismType) {
		this.location = location;
		this.mechanismType = mechanismType;
	}

	public void setMechanismType(MechanismType mechanismType) {
		this.mechanismType = mechanismType;
	}
}
