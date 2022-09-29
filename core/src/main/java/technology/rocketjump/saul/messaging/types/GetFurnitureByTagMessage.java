package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.tags.Tag;

import java.util.Collection;
import java.util.function.Consumer;

public record GetFurnitureByTagMessage(Class<? extends Tag> type, Consumer<Collection<Entity>> callback) {
}
