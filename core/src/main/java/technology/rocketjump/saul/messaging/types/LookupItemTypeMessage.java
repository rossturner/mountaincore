package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.ai.goap.actions.ItemTypeLookupCallback;

public class LookupItemTypeMessage {

    public final String typeName;
    public final ItemTypeLookupCallback callback;

    public LookupItemTypeMessage(String typeName, ItemTypeLookupCallback callback) {
        this.typeName = typeName;
        this.callback = callback;
    }
}
