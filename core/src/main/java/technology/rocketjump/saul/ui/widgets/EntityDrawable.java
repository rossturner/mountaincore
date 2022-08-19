package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;

import static technology.rocketjump.saul.rendering.entities.EntityRenderer.PIXELS_PER_TILE;

public class EntityDrawable extends BaseDrawable {

    private final Entity entity;
    private final EntityRenderer entityRenderer;
    private final MessageDispatcher messageDispatcher;
    private Color overrideColor = null;

    private final boolean showItemAsPlacedOnGround;
    private ItemPlacement revertToItemPlacement;

    public EntityDrawable(Entity entity, EntityRenderer entityRenderer, boolean showItemAsPlacedOnGround, MessageDispatcher messageDispatcher) {
        this.entity = entity;
        this.entityRenderer = entityRenderer;
        this.showItemAsPlacedOnGround = showItemAsPlacedOnGround;
        this.messageDispatcher = messageDispatcher;

        setMinWidth(PIXELS_PER_TILE);
        setMinHeight(PIXELS_PER_TILE);
    }

    @Override
    public void draw (Batch batch, float x, float y, float width, float height) {
        LocationComponent originalLocationComponent = entity.getLocationComponent();
        LocationComponent overrideLocationComponent = originalLocationComponent.clone(null, null);
        Vector2 screenPosition = new Vector2(x + (width / 2), y + (width / 2));
        overrideLocationComponent.setWorldPosition(screenPosition, false);
        entity.setLocationComponent(overrideLocationComponent);

        float tempPixelsPerTile = PIXELS_PER_TILE;
        PIXELS_PER_TILE = 1f;

        revertToItemPlacement = null;
        if (showItemAsPlacedOnGround && entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
            ItemPlacement currentItemPlacement = attributes.getItemPlacement();
            if (!ItemPlacement.ON_GROUND.equals(currentItemPlacement)) {
                revertToItemPlacement = currentItemPlacement;
                attributes.setItemPlacement(ItemPlacement.ON_GROUND);
                messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
            }
        }

        entityRenderer.render(entity, batch, RenderMode.DIFFUSE, null, overrideColor, null);

        if (revertToItemPlacement != null) {
            ((ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).setItemPlacement(revertToItemPlacement);
            messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
        }

        PIXELS_PER_TILE = tempPixelsPerTile;

        entity.setLocationComponent(originalLocationComponent);
    }

    public Color getOverrideColor() {
        return overrideColor;
    }

    public void setOverrideColor(Color overrideColor) {
        this.overrideColor = overrideColor;
    }
}
