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
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kotcrab.vis.ui.VisUI;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;

import java.util.Random;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

public class AssetEditorApplication extends ApplicationAdapter implements Telegraph {

	private MessageDispatcher messageDispatcher;
	private AssetEditorUI ui;

	private OrthographicCamera camera;
	private SpriteBatch spriteBatch;
	private ShapeRenderer shapeRenderer;
	private EntityRenderer entityRenderer;

	private NativeFileChooser fileChooser;

	private RaceDictionary raceDictionary;
	private CreatureEntityFactory creatureEntityFactory;

	private Entity entity;
	private Random random;
	private GameContext gameContext;


	@Inject
	public AssetEditorApplication(NativeFileChooser fileChooser) {
		this.fileChooser = fileChooser;
	}

	@Override
	public void create() {
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

		messageDispatcher = injector.getInstance(MessageDispatcher.class);
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED);
		messageDispatcher.addListener(this, MessageType.EDITOR_ENTITY_SELECTION);

		ui = injector.getInstance(AssetEditorUI.class);
		raceDictionary = injector.getInstance(RaceDictionary.class);
		creatureEntityFactory = injector.getInstance(CreatureEntityFactory.class); //TODO: speak to ross about abstract parent factory

		ViewAreaInputHandler viewAreaInputHandler = injector.getInstance(ViewAreaInputHandler.class);
		spriteBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		entityRenderer = injector.getInstance(EntityRenderer.class);

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(ui.getStage());
		inputMultiplexer.addProcessor(viewAreaInputHandler);
		Gdx.input.setInputProcessor(inputMultiplexer);
		this.random = new Random();
		this.gameContext = new GameContext();
		gameContext.setRandom(new RandomXS128());
	}

	@Override
	public void render () {
		camera.update();

		renderBackground();

		if (entity != null) {
			spriteBatch.begin();
			spriteBatch.setProjectionMatrix(camera.combined);

			//TODO: switch based on available orientations
			Vector2 originalPosition = entity.getLocationComponent().getWorldPosition().cpy();

			renderEntityWithOrientation(originalPosition, DOWN.toVector2(), 0, 0, RenderMode.DIFFUSE);
			renderEntityWithOrientation(originalPosition, DOWN_LEFT.toVector2(), -1, 0, RenderMode.DIFFUSE);
			renderEntityWithOrientation(originalPosition, DOWN_RIGHT.toVector2(), 1, 0, RenderMode.DIFFUSE);
			renderEntityWithOrientation(originalPosition, UP.toVector2(), 0, 1, RenderMode.DIFFUSE);
			renderEntityWithOrientation(originalPosition, UP_LEFT.toVector2(), -1, 1, RenderMode.DIFFUSE);
			renderEntityWithOrientation(originalPosition, UP_RIGHT.toVector2(), 1, 1, RenderMode.DIFFUSE);


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

	private void renderEntityWithOrientation(Vector2 originalPosition, Vector2 orientation, float offsetX, float offsetY, RenderMode renderMode) {
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
			case MessageType.EDITOR_ENTITY_SELECTION: {
				if (msg.extraInfo instanceof EditorEntitySelection entitySelection) {
					switch (entitySelection.getEntityType()) {
						case CREATURE -> {
							Race race = raceDictionary.getByName(entitySelection.getTypeName());
							Vector2 facing = new Vector2(0, 0f);
							Vector2 position = new Vector2((int)Math.floor(camera.viewportWidth * 0.5f) + 0.5f, (int)Math.floor(camera.viewportHeight * 0.4f) + 0.5f);
							CreatureEntityAttributes attributes = new CreatureEntityAttributes(race, random.nextLong());
							entity = creatureEntityFactory.create(attributes, position, facing, gameContext);
						}
						case PLANT -> {
						}
						case ITEM -> {
						}
						case FURNITURE -> {
						}
						case ONGOING_EFFECT -> {
						}
						case MECHANISM -> {
						}
					}
				}
			}
		}
		return true;
	}
}