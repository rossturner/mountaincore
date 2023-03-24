package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.tags.Tag;

import java.util.Collection;
import java.util.function.Consumer;

public record GetFurnitureByTagMessage(Class<? extends Tag> type, Consumer<Collection<Entity>> callback) {
}
