package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.model.Entity;

public record FactionChangedMessage(Entity entity, Faction currentFaction, Faction newFaction) {}
