package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kotcrab.vis.ui.VisUI;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.guice.SaulGuiceModule;

public class AssetEditorApplication extends ApplicationAdapter {

	private AssetEditorUI ui;

	private OrthographicCamera camera;
	private ShapeRenderer shapeRenderer;

	private NativeFileChooser fileChooser;

	private Entity entity;

	@Inject
	public AssetEditorApplication(NativeFileChooser fileChooser) {
		this.fileChooser = fileChooser;
	}

	@Override
	public void create () {
		VisUI.load();

		this.camera = new OrthographicCamera(Gdx.graphics.getWidth() / 100f, Gdx.graphics.getHeight() / 100f);
		camera.zoom = 0.5f;
		camera.position.x = camera.viewportWidth / 2;
		camera.position.y = camera.viewportHeight / 2;
		Injector injector = Guice.createInjector(new SaulGuiceModule() {
			@Override
			public void configure() {
				super.configure();
				bind(NativeFileChooser.class).toInstance(fileChooser);
				bind(OrthographicCamera.class).toInstance(camera);
			}
		});

		ui = injector.getInstance(AssetEditorUI.class);
		ViewAreaInputHandler viewAreaInputHandler = injector.getInstance(ViewAreaInputHandler.class);
		shapeRenderer = new ShapeRenderer();

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(ui.getStage());
		inputMultiplexer.addProcessor(viewAreaInputHandler);
		Gdx.input.setInputProcessor(inputMultiplexer);
	}

	@Override
	public void render () {
		camera.update();

		renderBackground();

		ui.render();
	}

	private void renderBackground() {
		Gdx.gl.glClearColor(0.6f, 0.6f, 0.6f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1f);
		shapeRenderer.setProjectionMatrix(camera.combined);
		for (float x = 0; x <= camera.viewportWidth + 1f; x += 0.5f) {
			for (float y = 0; y <= camera.viewportHeight + 1f; y += 0.5f) {
				boolean xEven = Math.round(x) - x < 0.001f;
				boolean yEven = Math.round(y) - y < 0.001f;

				if ((xEven && !yEven) || (!xEven && yEven)) {
					shapeRenderer.rect(x, y, 0.5f, 0.5f);
				}
			}
		}
		shapeRenderer.end();
	}

	@Override
	public void resize (int width, int height) {
		ui.onResize(width, height);

		float tempX = camera.position.x;
		float tempY = camera.position.y;
		float zoom = camera.zoom;
		camera.setToOrtho(false, width / 100f, height / 100f);
		camera.position.x = tempX;
		camera.position.y = tempY;
		camera.zoom = zoom;

//		screenWriter.onResize(width, height);

//		Vector3 newPosition = new Vector3(width, height, 0);
//		newPosition.x = newPosition.x * 0.65f;
//		newPosition.y = newPosition.y * 0.4f;
//		cameraManager.getCamera().unproject(newPosition);
//		// Round to nearest tile boundary
//		newPosition.x = Math.round(newPosition.x);
//		newPosition.y = Math.round(newPosition.y);
//		currentEntity.getLocationComponent().setWorldPosition(new Vector2(newPosition.x, newPosition.y), false);
	}

	@Override
	public void dispose () {
		VisUI.dispose();
	}
}