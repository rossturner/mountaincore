package technology.rocketjump.saul.assets.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kotcrab.vis.ui.VisUI;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

public class AssetEditorApplication extends ApplicationAdapter implements Telegraph {

	private AssetEditorUI ui;
	private OrthographicCamera camera;
	private SpriteBatch spriteBatch;
	private ShapeRenderer shapeRenderer;
	private EntityRenderer entityRenderer;

	private NativeFileChooser fileChooser;
	private EditorStateProvider editorStateProvider;

	@Inject
	public AssetEditorApplication(NativeFileChooser fileChooser) {
		this.fileChooser = fileChooser;
	}

	@Override
	public void create() {
		VisUI.load();
		this.spriteBatch = new SpriteBatch();
		this.shapeRenderer = new ShapeRenderer();

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

		entityRenderer = injector.getInstance(EntityRenderer.class);
		editorStateProvider = injector.getInstance(EditorStateProvider.class);
		ui = injector.getInstance(AssetEditorUI.class);

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(ui.getStage());
		inputMultiplexer.addProcessor(injector.getInstance(ViewAreaInputHandler.class));
		Gdx.input.setInputProcessor(inputMultiplexer);

		MessageDispatcher messageDispatcher = injector.getInstance(MessageDispatcher.class);
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED);
	}

	@Override
	public void render () {
		camera.update();

		renderBackground();
		Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
		if (currentEntity != null) {
			spriteBatch.begin();
			spriteBatch.setProjectionMatrix(camera.combined);

			//TODO: switch based on available orientations
			Vector2 originalPosition = new Vector2((int)Math.floor(camera.viewportWidth * 0.5f) + 0.5f, (int)Math.floor(camera.viewportHeight * 0.4f) + 0.5f);
//			Vector2 originalPosition = currentEntity.getLocationComponent().getWorldPosition().cpy();

			RenderMode currentRenderMode = editorStateProvider.getState().getRenderMode();
			renderEntityWithOrientation(currentEntity, originalPosition, DOWN.toVector2(), 0, 0, currentRenderMode);
			renderEntityWithOrientation(currentEntity, originalPosition, DOWN_LEFT.toVector2(), -1, 0, currentRenderMode);
			renderEntityWithOrientation(currentEntity, originalPosition, DOWN_RIGHT.toVector2(), 1, 0, currentRenderMode);
			renderEntityWithOrientation(currentEntity, originalPosition, UP.toVector2(), 0, 1, currentRenderMode);
			renderEntityWithOrientation(currentEntity, originalPosition, UP_LEFT.toVector2(), -1, 1, currentRenderMode);
			renderEntityWithOrientation(currentEntity, originalPosition, UP_RIGHT.toVector2(), 1, 1, currentRenderMode);

			spriteBatch.end();
		}


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

	private void renderEntityWithOrientation(Entity entity, Vector2 originalPosition, Vector2 orientation, float offsetX, float offsetY, RenderMode renderMode) {
		// Set orientation
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(orientation), true, false);
		// Set position
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(offsetX, offsetY), false, false);
		// Render
		entityRenderer.render(entity, spriteBatch, renderMode, null, null, null);
		// Reset position
		entity.getLocationComponent().setWorldPosition(originalPosition, false, false);
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
		spriteBatch.dispose();
		VisUI.dispose();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ENTITY_CREATED: {
				//do nothing
			}
		}
		return true;
	}

}