package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kotcrab.vis.ui.VisUI;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.editor.components.propertyeditor.ComponentBuilder;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.rendering.camera.PrimaryCameraWrapper;

public class AssetEditorApplication extends ApplicationAdapter {

	private AssetEditorUI ui;

	private PrimaryCameraWrapper cameraManager;
	private ShapeRenderer shapeRenderer;

	private NativeFileChooser fileChooser;

	@Inject
	public AssetEditorApplication(NativeFileChooser fileChooser) {
		this.fileChooser = fileChooser;
	}

	@Override
	public void create () {
		VisUI.load();
		ComponentBuilder.staticShapeRenderer = new ShapeRenderer();
		Injector injector = Guice.createInjector(new SaulGuiceModule() {
			@Override
			public void configure() {
				super.configure();
				bind(NativeFileChooser.class).toInstance(fileChooser);
			}
		});

		this.cameraManager = injector.getInstance(PrimaryCameraWrapper.class);
		ui = injector.getInstance(AssetEditorUI.class);
		shapeRenderer = new ShapeRenderer();

		Gdx.input.setInputProcessor(ui.getStage());
	}

	@Override
	public void render () {
		renderBackground();

		ui.render();
	}

	private void renderBackground() {
		Gdx.gl.glClearColor(0.6f, 0.6f, 0.6f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(0.7f, 0.7f, 0.7f, 1f);
		shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);
		for (float x = 0; x <= cameraManager.getCamera().viewportWidth + 1f; x += 0.5f) {
			for (float y = 0; y <= cameraManager.getCamera().viewportHeight + 1f; y += 0.5f) {
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
		cameraManager.onResize(width, height);
		cameraManager.getCamera().zoom = 0.5f;
		cameraManager.getCamera().update();
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