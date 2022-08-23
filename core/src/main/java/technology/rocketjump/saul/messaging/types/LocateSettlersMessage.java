package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;

import java.util.List;
import java.util.function.Consumer;

public class LocateSettlersMessage {
    public final int regionId;
    public final Consumer<List<Entity>> callback;

    public LocateSettlersMessage(int regionId, Consumer<List<Entity>> callback) {
        this.regionId = regionId;
        this.callback = callback;
    }

}
