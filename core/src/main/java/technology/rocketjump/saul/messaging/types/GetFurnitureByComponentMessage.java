package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.model.Entity;

import java.util.Collection;
import java.util.function.Consumer;

public record GetFurnitureByComponentMessage(Class<? extends EntityComponent> type, Consumer<Collection<Entity>> callback) {
}
