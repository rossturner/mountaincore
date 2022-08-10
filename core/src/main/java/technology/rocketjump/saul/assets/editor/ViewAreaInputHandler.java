package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Inject;

public class ViewAreaInputHandler implements InputProcessor {

	private final OrthographicCamera camera;
	protected int button = -1;
	private float startX, startY;
	public float translateUnits = 10f;
	private final Vector3 tmpV1 = new Vector3();
	private final Vector3 tmpV2 = new Vector3();

	@Inject
	public ViewAreaInputHandler(OrthographicCamera camera) {
		this.camera = camera;
	}

	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		startX = screenX;
		startY = screenY;
		this.button = button;
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		this.button = -1;
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		//Copied from CameraInputController
		if (Input.Buttons.MIDDLE == button) {
			final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
			final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();
			startX = screenX;
			startY = screenY;

			float zoomedTranslation = translateUnits * camera.zoom;
			camera.translate(tmpV1.set(camera.direction).crs(camera.up).nor().scl(-deltaX * zoomedTranslation));
			camera.translate(tmpV2.set(camera.up).scl(-deltaY * zoomedTranslation));
		}
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		camera.zoom += (amount * 0.05f);
		camera.zoom = Math.max(0.1f, Math.min(1.2f, camera.zoom));
		return true;
	}
}
