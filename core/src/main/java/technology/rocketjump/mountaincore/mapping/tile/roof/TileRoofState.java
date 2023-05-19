package technology.rocketjump.mountaincore.mapping.tile.roof;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.constants.UiConstants;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

public enum TileRoofState {

    OPEN(HexColors.get("#54faed99")),
    CONSTRUCTED(HexColors.get("#463e3a99")),
    MINED(HexColors.get("#dfcfba99")),
    CAVERN(HexColors.get("#8aeab399")),
    MOUNTAIN_ROOF(HexColors.get("#aaaaaa99"));

    public Color viewColor;

    TileRoofState(Color viewColor) {
        this.viewColor = viewColor;
    }

    public static void initFromConstants(UiConstants uiConstants) {
        for (TileRoofState roofState : TileRoofState.values()) {
            String colorCode = uiConstants.getRoofStateColors().get(roofState.name());
            if (colorCode == null) {
                throw new RuntimeException("No color code for roof state: " + roofState.name());
            }
            roofState.viewColor = HexColors.get(colorCode);
        }
    }
}
