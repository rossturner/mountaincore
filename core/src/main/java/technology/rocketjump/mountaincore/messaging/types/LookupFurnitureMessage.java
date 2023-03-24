package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;

import java.util.function.Consumer;

public class LookupFurnitureMessage {

    public final String furnitureTypeName;
    public final Consumer<FurnitureType> callback;

    public LookupFurnitureMessage(String furnitureTypeName, Consumer<FurnitureType> callback) {
        this.furnitureTypeName = furnitureTypeName;
        this.callback = callback;
    }
}
