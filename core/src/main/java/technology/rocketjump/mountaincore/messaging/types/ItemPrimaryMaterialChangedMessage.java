package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

public class ItemPrimaryMaterialChangedMessage {

    public final Entity item;
    public final GameMaterial oldPrimaryMaterial;

    public ItemPrimaryMaterialChangedMessage(Entity item, GameMaterial oldPrimaryMaterial) {
        this.item = item;
        this.oldPrimaryMaterial = oldPrimaryMaterial;
    }
}
