package technology.rocketjump.mountaincore.mapping.minimap;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import technology.rocketjump.mountaincore.messaging.MessageType;

public class MinimapImage extends Container<Actor> {

	private final TextureRegionDrawable selectionDrawable;
	private TextureRegionDrawable minimapDrawable;

	private float cameraPositionX;
	private float cameraPositionY;
	private float viewportWidth;
	private float viewportHeight;
	private int mapWidth;
	private int mapHeight;

	public MinimapImage(TextureRegionDrawable selectionDrawable, MessageDispatcher messageDispatcher) {
		this.setTouchable(Touchable.enabled);
		this.selectionDrawable = selectionDrawable;

		this.addListener(new ClickListener() {
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, new Vector2(
						(x / getWidth()) * mapWidth,
						(y / getHeight()) * mapHeight
				));
				messageDispatcher.dispatchMessage(MessageType.TUTORIAL_TRACKING_MINIMAP_CLICKED);
				return super.touchDown(event, x, y, pointer, button);
			}

			@Override
			public void touchDragged (InputEvent event, float x, float y, int pointer) {
				messageDispatcher.dispatchMessage(MessageType.MOVE_CAMERA_TO, new Vector2(
						(x / getWidth()) * mapWidth,
						(y / getHeight()) * mapHeight
				));
				super.touchDragged(event, x, y, pointer);
			}
		});
	}

	public void updateTexture(Texture minimapTexture) {
		if (minimapDrawable == null || !minimapDrawable.getRegion().getTexture().equals(minimapTexture)) {
			this.minimapDrawable = new TextureRegionDrawable(new TextureRegion(minimapTexture));
			this.setBackground(minimapDrawable);
		}
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);

		float imageCameraPositionX = (cameraPositionX / mapWidth) * getWidth();
		float imageCameraPositionY = (cameraPositionY / mapHeight) * getHeight();
		float imageViewportWidth = (viewportWidth / mapWidth) * getWidth();
		float imageViewportHeight = (viewportHeight / mapHeight) * getHeight();

		float cameraImageMinX = imageCameraPositionX - (imageViewportWidth / 2);
		float cameaImageMinY = imageCameraPositionY - (imageViewportHeight / 2);

		selectionDrawable.draw(batch, getX() + cameraImageMinX, getY() + cameaImageMinY, imageViewportWidth, imageViewportHeight);

	}

	public void setCameraPosition(Vector3 position) {
		this.cameraPositionX = position.x;
		this.cameraPositionY = position.y;
	}

	public void setViewportSize(float viewportWidth, float viewportHeight) {
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
	}

	public void setMapSize(int mapWidth, int mapHeight) {
		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;
	}

}
