package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.model.Entity;

public record FactionChangedMessage(Entity entity, Faction currentFaction, Faction newFaction) {}
