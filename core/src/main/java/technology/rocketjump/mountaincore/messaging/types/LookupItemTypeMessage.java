package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.ai.goap.actions.ItemTypeLookupCallback;

public class LookupItemTypeMessage {

    public final String typeName;
    public final ItemTypeLookupCallback callback;

    public LookupItemTypeMessage(String typeName, ItemTypeLookupCallback callback) {
        this.typeName = typeName;
        this.callback = callback;
    }
}
