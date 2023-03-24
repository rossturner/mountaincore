package technology.rocketjump.mountaincore.messaging.types;

import java.util.function.Consumer;

public class LookupMessage<T> {

    public final String typeName;
    public final Consumer<T> callback;

    public LookupMessage(String typeName, Consumer<T> callback) {
        this.typeName = typeName;
        this.callback = callback;
    }
}
