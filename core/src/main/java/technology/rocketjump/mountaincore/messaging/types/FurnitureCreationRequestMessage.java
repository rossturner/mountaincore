package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class FurnitureCreationRequestMessage {

	public final FurnitureEntityAttributes furnitureAttributes;
	public final Map<Long, Entity> inputItems;
	public final GridPoint2 primarylocation;
	public final Set<GridPoint2> tileLocations;
	public final Consumer<Entity> callback;


	public FurnitureCreationRequestMessage(FurnitureEntityAttributes furnitureAttributes, Map<Long, Entity> inputItems, GridPoint2 primarylocation, Set<GridPoint2> tileLocations, Consumer<Entity> callback) {
		this.furnitureAttributes = furnitureAttributes;
		this.inputItems = inputItems;
		this.primarylocation = primarylocation;
		this.tileLocations = tileLocations;
		this.callback = callback;
	}
}
