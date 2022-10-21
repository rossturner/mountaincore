package technology.rocketjump.saul.mapping.minimap;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.ray3k.tenpatch.TenPatchDrawable;

public class MinimapContainer extends Container<MinimapImage> {

	private final MinimapImage minimapImage;

	private static final float MINIMAP_FRAME_BORDER_SIZE = 41f / 2f;

	private float containerWidth;
	private float containerHeight;

	public MinimapContainer(TextureRegionDrawable selectionDrawable, MessageDispatcher messageDispatcher, Skin skin) {
		this.setTouchable(Touchable.enabled);

		TenPatchDrawable backgroundDrawable = skin.get("map_bg_full_patch", TenPatchDrawable.class);
		backgroundDrawable.setScale(0.5f);
		this.setBackground(backgroundDrawable, false);
		this.pad(41f / 2f);

		this.minimapImage = new MinimapImage(selectionDrawable, messageDispatcher, skin);
		this.setActor(this.minimapImage);
	}


	public void updateTexture(Texture minimapTexture) {
		minimapImage.updateTexture(minimapTexture);
	}

	public void setCameraPosition(Vector3 position) {
		minimapImage.setCameraPosition(position);
	}

	public void setViewportSize(float viewportWidth, float viewportHeight) {
		minimapImage.setViewportSize(viewportWidth, viewportHeight);
	}

	public void setMapSize(int mapWidth, int mapHeight) {
		minimapImage.setMapSize(mapWidth, mapHeight);
	}

	public float getContainerWidth() {
		return containerWidth;
	}

	public void setContainerWidth(float containerWidth, boolean addBorder) {
		this.containerWidth = containerWidth;
		if (addBorder) {
			this.containerWidth += (2 * MINIMAP_FRAME_BORDER_SIZE);
		}
	}

	public float getContainerHeight() {
		return containerHeight;
	}

	public void setContainerHeight(float containerHeight, boolean addBorder) {
		this.containerHeight = containerHeight;
		if (addBorder) {
			this.containerHeight += (2 * MINIMAP_FRAME_BORDER_SIZE);
		}
	}

 	@Override
	public void setSize (float width, float height) {
		super.setSize(width, height);
		minimapImage.setSize(width - (2 * MINIMAP_FRAME_BORDER_SIZE), height - (2 * MINIMAP_FRAME_BORDER_SIZE));
	}
}
