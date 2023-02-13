package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.saul.rendering.entities.EntityRenderer.PIXELS_PER_TILE;

public class EntityDrawable extends BaseDrawable {

    private final Entity entity;
    private final EntityRenderer entityRenderer;
    private final MessageDispatcher messageDispatcher;
    private Color overrideColor = null;

    private final boolean showItemAsPlacedOnGround;
    private final Vector2 screenPositionOffset = new Vector2();
    private ItemPlacement revertToItemPlacement;
    private Drawable backgroundDrawable;

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
        if (backgroundDrawable != null) {
            backgroundDrawable.draw(batch, x, y, width, height);
        }

        if (entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes && attributes.getQuantity() <= 0) {
            return;
        }

        LocationComponent originalLocationComponent = entity.getLocationComponent(true);
        LocationComponent overrideLocationComponent = originalLocationComponent.clone(null, null);
        Vector2 screenPosition = new Vector2(x + (width / 2) + screenPositionOffset.x, y + (width / 2) + screenPositionOffset.y);
        overrideLocationComponent.setWorldPosition(screenPosition, false);
        overrideLocationComponent.setFacing(DOWN.toVector2());
        entity.setLocationComponent(overrideLocationComponent);

        float originalPixelsPerTile = PIXELS_PER_TILE;
        PIXELS_PER_TILE = originalPixelsPerTile / width;
        if (entity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes furnitureEntityAttributes) {
            FurnitureLayout layout = furnitureEntityAttributes.getCurrentLayout();
            adjustPositionForLayout(layout, x, y, width, height, overrideLocationComponent);
        }

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

        PIXELS_PER_TILE = originalPixelsPerTile;

        entity.setLocationComponent(originalLocationComponent);
    }

    private void adjustPositionForLayout(FurnitureLayout layout, float x, float y, float width, float height, LocationComponent overrideLocationComponent) {
        int minX = 0, maxX = 0, minY = 0, maxY = 0;
        for (GridPoint2 extraTile : layout.getExtraTiles()) {
            minX = Math.min(minX, extraTile.x);
            minY = Math.min(minY, extraTile.y);
            maxX = Math.max(maxX, extraTile.x);
            maxY = Math.max(maxY, extraTile.y);
        }

        int tileWidth = 1 + maxX - minX;
        int tileHeight = 1 + maxY - minY;
        int maxTileAmount = Math.max(tileWidth, tileHeight);
        // scale down a bit more
        float amountAsFloat = 1.05f * (float)maxTileAmount;
        if (tileWidth == 1 && tileHeight == 1) {
            amountAsFloat *= 1.35f; // make 1x1 furniture smaller to match with the others
        }

        PIXELS_PER_TILE = PIXELS_PER_TILE * amountAsFloat;

        // Also need to offset the position based on the tile width and height
        float tileSegmentWidth = width / maxTileAmount;
        float tileSegmentHeight = height / maxTileAmount;

        Vector2 positionFromCorner = new Vector2(tileSegmentWidth / 2, tileSegmentHeight / 2f);
        while (minX < 0) {
            positionFromCorner.x += tileSegmentWidth;
            minX++;
        }
        while (minY < 0) {
            positionFromCorner.y += tileSegmentHeight;
            minY++;
        }

        // further adjustment to center non-square layouts
        while (tileWidth > tileHeight) {
            positionFromCorner.y += tileSegmentHeight / 2;
            tileHeight++;
        }
        while (tileHeight > tileWidth) {
            positionFromCorner.x += tileSegmentWidth / 2;
            tileWidth++;
        }

        overrideLocationComponent.setWorldPosition(positionFromCorner.add(x, y).add(screenPositionOffset), false);
    }

    public Color getOverrideColor() {
        return overrideColor;
    }

    public void setOverrideColor(Color overrideColor) {
        this.overrideColor = overrideColor;
    }

    public EntityDrawable withBackground(Drawable backgroundDrawable) {
        this.backgroundDrawable = backgroundDrawable;
        return this;
    }

    public void setScreenPositionOffset(float x, float y) {
        this.screenPositionOffset.set(x, y);
    }
}
