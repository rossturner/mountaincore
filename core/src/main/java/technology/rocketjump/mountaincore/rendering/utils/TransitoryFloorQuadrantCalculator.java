package technology.rocketjump.mountaincore.rendering.utils;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.MapVertex;
import technology.rocketjump.mountaincore.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.mountaincore.mapping.tile.floor.TileFloor;

public class TransitoryFloorQuadrantCalculator {

    private static class NullCalculator extends TransitoryFloorQuadrantCalculator {
        @Override
        protected Color[] adjust(Color[] vertexColors, Quadrant quadrant) {
            return vertexColors;
        }
    }

    private static class NormalRenderModeCalculator extends TransitoryFloorQuadrantCalculator {
        private static final Color[] WHITE_QUADRANT_COLORS = {Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE};
        @Override
        protected Color[] adjust(Color[] vertexColors, Quadrant quadrant) {
            return WHITE_QUADRANT_COLORS;
        }
    }

    public static final TransitoryFloorQuadrantCalculator NULL_CALCULATOR = new NullCalculator();
    public static final NormalRenderModeCalculator NORMAL_RENDER_MODE_CALCULATOR = new NormalRenderModeCalculator();

    private float bottomLeft;
    private float topLeft;
    private float topRight;
    private float bottomRight;
    private float leftMiddle;
    private float topMiddle;
    private float rightMiddle;
    private float bottomMiddle;
    private float middle;

    private TransitoryFloorQuadrantCalculator() {

    }

    public TransitoryFloorQuadrantCalculator(MapTile mapTile, GameContext gameContext) {

        MapVertex[] vertices =  gameContext.getAreaMap().getVertices(mapTile.getTileX(), mapTile.getTileY());
        bottomLeft = vertices[0].getTransitoryFloorAlpha();
        topLeft = vertices[1].getTransitoryFloorAlpha();
        topRight = vertices[2].getTransitoryFloorAlpha();
        bottomRight = vertices[3].getTransitoryFloorAlpha();

        leftMiddle = (topLeft + bottomLeft)/2.0f;
        topMiddle = (topLeft + topRight)/2.0f;
        rightMiddle = (topRight + bottomRight)/2.0f;
        bottomMiddle = (bottomLeft + bottomRight)/2.0f;

        middle = (bottomLeft + bottomRight + topLeft + topRight) / 4.0f;
    }

    public enum Quadrant {
        ZERO, ONE, TWO, THREE;
    }

    /**
     * Beware, mutates color array, for performance reasons
     * @param floor
     * @param quadrant
     */
    public Color[] adjustAlpha(TileFloor floor, Quadrant quadrant) {
        if (floor.getFloorType().isUseMaterialColor() && floor.getMaterial().getColor() != null) {
            Color materialColor = floor.getMaterial().getColor();
            return  new Color[] {materialColor, materialColor, materialColor, materialColor};
        }

        Color[] vertexColors = floor.getVertexColors();
        return adjust(vertexColors, quadrant);
    }

    /**
     * Beware, mutates color array, for performance reasons
     * @param floorOverlap
     * @param quadrant
     * @return
     */
    public Color[] adjustAlpha(FloorOverlap floorOverlap, Quadrant quadrant) {
        Color[] vertexColors = floorOverlap.getVertexColors();
        return adjust(vertexColors, quadrant);
    }

    protected Color[] adjust(Color[] vertexColors, Quadrant quadrant) {
        switch (quadrant) {
            case ZERO -> {
                vertexColors[0].a = leftMiddle;
                vertexColors[1].a = topLeft;
                vertexColors[2].a = topMiddle;
                vertexColors[3].a = middle;
            }
            case ONE -> {
                vertexColors[0].a = middle;
                vertexColors[1].a = topMiddle;
                vertexColors[2].a = topRight;
                vertexColors[3].a = rightMiddle;
            }
            case TWO -> {
                vertexColors[0].a = bottomLeft;
                vertexColors[1].a = leftMiddle;
                vertexColors[2].a = middle;
                vertexColors[3].a = bottomMiddle;
            }
            case THREE -> {
                vertexColors[0].a = bottomMiddle;
                vertexColors[1].a = middle;
                vertexColors[2].a = rightMiddle;
                vertexColors[3].a = bottomRight;
            }
        }
        return vertexColors;
    }
}
