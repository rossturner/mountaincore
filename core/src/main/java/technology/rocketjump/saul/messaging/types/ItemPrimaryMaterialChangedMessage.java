package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class ItemPrimaryMaterialChangedMessage {

    public final Entity item;
    public final GameMaterial oldPrimaryMaterial;

    public ItemPrimaryMaterialChangedMessage(Entity item, GameMaterial oldPrimaryMaterial) {
        this.item = item;
        this.oldPrimaryMaterial = oldPrimaryMaterial;
    }
}
