package technology.rocketjump.saul.ui;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.persistence.UserPreferences;

public class ViewportUtils {

    public static final float MIN_VIEWPORT_SCALE = 0.9f;
    public static final float MAX_VIEWPORT_SCALE = 1.15f;
    private final static Vector2 VIEWPORT_DIMENSIONS = new Vector2(3840, 2160);

    public static Vector2 scaledViewportDimensions(UserPreferences userPreferences) {
        return VIEWPORT_DIMENSIONS.cpy().scl(getViewportScale(userPreferences));
    }

    public static float defaultFontScaleForViewportScale(UserPreferences userPreferences) {
        //calculation for the defaultFontScale
        float maxV = ViewportUtils.MAX_VIEWPORT_SCALE;
        float minV = ViewportUtils.MIN_VIEWPORT_SCALE;
        float midpoint = minV + ((maxV - minV)/2.0f);
        float diffFromMidpoint = getViewportScale(userPreferences) - midpoint;

        final float percentAlongViewportDomain;
        if (diffFromMidpoint < 0) {
            percentAlongViewportDomain = Math.abs(diffFromMidpoint) / (midpoint - minV);
        } else {
            percentAlongViewportDomain = diffFromMidpoint / (maxV - midpoint);
        }
        return 2.0f + (percentAlongViewportDomain * (2.6f - 2.0f));
    }

    private static Float getViewportScale(UserPreferences userPreferences) {
        String preference = userPreferences.getPreference(UserPreferences.PreferenceKey.UI_SCALE, "1.0f");
        return Float.valueOf(preference);
    }
}
