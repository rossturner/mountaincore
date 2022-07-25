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
import com.google.inject.multibindings.MapBinder;
import com.kotcrab.vis.ui.VisUI;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.editor.factory.CreatureUIFactory;
import technology.rocketjump.saul.assets.editor.factory.UIFactory;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.utils.HexColors;

public class AssetEditorApplication extends ApplicationAdapter implements Telegraph {

	private AssetEditorUI ui;
	private OrthographicCamera camera;
	private SpriteBatch spriteBatch;
	private ShapeRenderer shapeRenderer;
	private EntityRenderer entityRenderer;

	private NativeFileChooser fileChooser;
	private EditorStateProvider editorStateProvider;
	private EntityAssetUpdater entityAssetUpdater;

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
				MapBinder<EntityType, UIFactory> uiFactoryMapBinder = MapBinder.newMapBinder(binder(), EntityType.class, UIFactory.class);
				uiFactoryMapBinder.addBinding(EntityType.CREATURE).to(CreatureUIFactory.class);
			}
		});
		entityAssetUpdater = injector.getInstance(EntityAssetUpdater.class);
		entityRenderer = injector.getInstance(EntityRenderer.class);
		editorStateProvider = injector.getInstance(EditorStateProvider.class);
		ui = injector.getInstance(AssetEditorUI.class);

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(ui.getStage());
		inputMultiplexer.addProcessor(injector.getInstance(ViewAreaInputHandler.class));
		Gdx.input.setInputProcessor(inputMultiplexer);

		MessageDispatcher messageDispatcher = injector.getInstance(MessageDispatcher.class);
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED);
		messageDispatcher.addListener(this, MessageType.ENTITY_ASSET_UPDATE_REQUIRED);
	}

	@Override
	public void render () {
		camera.update();

		renderBackground();
		Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
		if (currentEntity != null) {
			EntityAsset baseAsset = currentEntity.getPhysicalEntityComponent().getBaseAsset();
			if (baseAsset != null) { //Don't render without the base asset, this can be for newly created entities
				Vector2 originalPosition = new Vector2((int)Math.floor(camera.viewportWidth * 0.5f) + 0.5f, (int)Math.floor(camera.viewportHeight * 0.4f) + 0.5f);

				RenderMode currentRenderMode = editorStateProvider.getState().getRenderMode();

				//TODO: this isn't my best code, learn to do it properly - Rocky
				//render boxes
				for (EntityAssetOrientation orientation : EntityAssetOrientation.values()) {
					if (baseAsset.getSpriteDescriptors().containsKey(orientation) && baseAsset.getSpriteDescriptors().get(orientation).getSprite(currentRenderMode) != null) {
						renderEntityWithOrientation(currentEntity, orientation, originalPosition, currentRenderMode, false);
					}
				}

				for (EntityAssetOrientation orientation : EntityAssetOrientation.values()) {
					if (baseAsset.getSpriteDescriptors().containsKey(orientation) && baseAsset.getSpriteDescriptors().get(orientation).getSprite(currentRenderMode) != null) {
						renderEntityWithOrientation(currentEntity, orientation, originalPosition, currentRenderMode, true);
					}
				}
			}
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

	private void renderEntityWithOrientation(Entity entity, EntityAssetOrientation orientation, Vector2 originalPosition, RenderMode renderMode, boolean renderSprite) {
		int spritePadding = editorStateProvider.getState().getSpritePadding();
		float offsetX = orientation.asOriginalVector.x * spritePadding;
		float offsetY = Math.max(orientation.asOriginalVector.y, 0) * spritePadding;

		// Set orientation
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(orientation.asOriginalVector), true, false);
		// Set position
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(offsetX, offsetY), false, false);

		if (renderSprite) {
			// Render
			spriteBatch.begin();
			spriteBatch.setProjectionMatrix(camera.combined);
			entityRenderer.render(entity, spriteBatch, renderMode, null, null, null);
			spriteBatch.end();
		} else {
			// Render outling around actual entity position
			shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
			shapeRenderer.setColor(HexColors.get("#3355BB"));
			shapeRenderer.rect(entity.getLocationComponent().getWorldPosition().x - 0.5f, entity.getLocationComponent().getWorldPosition().y - 0.5f,1, 1);
			shapeRenderer.end();
		}
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
				return true;
			}
			case MessageType.ENTITY_ASSET_UPDATE_REQUIRED: {
				entityAssetUpdater.updateEntityAssets((Entity) msg.extraInfo);
				return true;
			}
		}
		return true;
	}

}