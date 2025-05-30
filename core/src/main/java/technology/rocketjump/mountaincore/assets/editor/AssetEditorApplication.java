package technology.rocketjump.mountaincore.assets.editor;

import com.badlogic.gdx.*;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.kotcrab.vis.ui.FocusManager;
import com.kotcrab.vis.ui.VisUI;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.mountaincore.AssetsPackager;
import technology.rocketjump.mountaincore.assets.editor.factory.*;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.audio.AudioMessageHandler;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.components.AnimationComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.guice.MountaincoreGuiceModule;
import technology.rocketjump.mountaincore.logging.CrashHandler;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.CameraMovedMessage;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.particles.ParticleEffectUpdater;
import technology.rocketjump.mountaincore.rendering.RenderMode;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.rendering.entities.EntityRenderer;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

import java.util.function.Consumer;

public class AssetEditorApplication extends ApplicationAdapter implements Telegraph {

	public static final Color ENTITY_OUTLINE_COLOR = HexColors.get("#3355BB");
	private AssetEditorUI ui;
	private OrthographicCamera camera;
	private SpriteBatch spriteBatch;
	private ShapeRenderer shapeRenderer;
	private EntityRenderer entityRenderer;

	private NativeFileChooser fileChooser;
	private EditorStateProvider editorStateProvider;
	private EntityAssetUpdater entityAssetUpdater;
	private CreatureUIFactory creatureUIFactory;
	private MessageDispatcher messageDispatcher;
	private Entity itemHoldingDwarf;
	private static final Color WORKSPACE_TILE_COLOR = new Color(1f, 0f, 1f, 0.2f);
	private static final Color WORKSPACE_OFFSET_TILE_COLOR = new Color(1f, 0f, 0f, 0.2f);

	@Inject
	public AssetEditorApplication(NativeFileChooser fileChooser) {
		this.fileChooser = fileChooser;
	}

	@Override
	public void create() {
		try {
			VisUI.load();
			this.spriteBatch = new SpriteBatch();
			this.shapeRenderer = new ShapeRenderer();

			this.camera = new OrthographicCamera(Gdx.graphics.getWidth() / 100f, Gdx.graphics.getHeight() / 100f);
			camera.zoom = 0.5f;
			camera.position.x = camera.viewportWidth / 2;
			camera.position.y = camera.viewportHeight / 2;
			init(true);
		} catch (Throwable e) {
			CrashHandler.logCrash(e);
		}
	}

	private void init(boolean firstLoad) {
		if (ui != null) {
			FocusManager.resetFocus(ui.getStage()); //Memory saving trick, otherwise the old stage could've kept reference to old ItemEntityAssetDictionary
		}

		Injector injector = Guice.createInjector(new MountaincoreGuiceModule() {
			@Override
			public void configure() {
				super.configure();
				bind(NativeFileChooser.class).toInstance(fileChooser);
				bind(OrthographicCamera.class).toInstance(camera);
				MapBinder<EntityType, UIFactory> uiFactoryMapBinder = MapBinder.newMapBinder(binder(), EntityType.class, UIFactory.class);
				uiFactoryMapBinder.addBinding(EntityType.CREATURE).to(CreatureUIFactory.class);
				uiFactoryMapBinder.addBinding(EntityType.ITEM).to(ItemUIFactory.class);
				uiFactoryMapBinder.addBinding(EntityType.FURNITURE).to(FurnitureUIFactory.class);
				uiFactoryMapBinder.addBinding(EntityType.VEHICLE).to(VehicleUIFactory.class);
				uiFactoryMapBinder.addBinding(EntityType.PLANT).to(PlantUIFactory.class);
			}
		});
		injector.getInstance(CrashHandler.class); //ensure we load user preferences for crash
		messageDispatcher = injector.getInstance(MessageDispatcher.class);
		//Do nothing handlers
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED);
		messageDispatcher.addListener(this, MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME);
		messageDispatcher.addListener(this, MessageType.IDENTIFY_PARTICLE_REQUEST_TARGET);

		entityAssetUpdater = injector.getInstance(EntityAssetUpdater.class);
		entityRenderer = injector.getInstance(EntityRenderer.class);
		editorStateProvider = injector.getInstance(EditorStateProvider.class);
		ui = injector.getInstance(AssetEditorUI.class);
		creatureUIFactory = injector.getInstance(CreatureUIFactory.class);

		injector.getInstance(AudioMessageHandler.class); //This handles sound effects in animation scrips
		injector.getInstance(ParticleEffectUpdater.class);
		itemHoldingDwarf = creatureUIFactory.createEntityForRendering("Dwarf");

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(ui.getStage());
		inputMultiplexer.addProcessor(injector.getInstance(ViewAreaInputHandler.class));
		inputMultiplexer.addProcessor(new InputAdapter() {
			@Override
			public boolean keyUp(int keycode) {
				if (keycode == Input.Keys.D && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
					GlobalSettings.DEV_MODE = !GlobalSettings.DEV_MODE;
					messageDispatcher.dispatchMessage(MessageType.DEV_MODE_CHANGED);
					return true;
				}
				return super.keyUp(keycode);
			}
		});
		Gdx.input.setInputProcessor(inputMultiplexer);

		messageDispatcher.dispatchMessage(MessageType.CAMERA_MOVED, new CameraMovedMessage(
				camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom, camera.position, new Vector2(0,0),
				13, 90)); //In order for sound effects to play, the audio system needs a camera placement


		messageDispatcher.addListener(this, MessageType.ENTITY_ASSET_UPDATE_REQUIRED);
		messageDispatcher.addListener(this, MessageType.EDITOR_RELOAD);

		if (firstLoad && editorStateProvider.getState().hasModSelected()) {
			//need to rerun assetPacker
			messageDispatcher.dispatchMessage(MessageType.EDITOR_OPEN_MOD, new FileHandle(editorStateProvider.getState().getModDirPath().toFile()));
		}
	}

	@Override
	public void render () {
		try {
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
						if (baseAsset.getSpriteDescriptors().containsKey(orientation) && hasSpriteForRenderMode(baseAsset, currentRenderMode, orientation)) {
							renderEntityWithOrientation(currentEntity, orientation, originalPosition, entity -> {
								Vector2 worldPosition = entity.getLocationComponent().getWorldPosition();
								shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
								shapeRenderer.setColor(ENTITY_OUTLINE_COLOR);
								shapeRenderer.rect(worldPosition.x - 0.5f, worldPosition.y - 0.5f,1, 1);
								shapeRenderer.end();
							});
						}
					}

					for (EntityAssetOrientation orientation : EntityAssetOrientation.values()) {
						if (baseAsset.getSpriteDescriptors().containsKey(orientation) && hasSpriteForRenderMode(baseAsset, currentRenderMode, orientation)) {

							Consumer<Entity> entityRenderer = entity -> {
								spriteBatch.begin();
								spriteBatch.setProjectionMatrix(camera.combined);
								this.entityRenderer.render(entity, spriteBatch, currentRenderMode, null, null, null);
								spriteBatch.end();
							};
							if (currentEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes iea && iea.getItemPlacement() == ItemPlacement.BEING_CARRIED) {
								EquippedItemComponent equippedItemComponent = itemHoldingDwarf.getOrCreateComponent(EquippedItemComponent.class);
								currentEntity.getOrCreateComponent(ItemAllocationComponent.class).init(currentEntity, messageDispatcher, new GameContext());
								equippedItemComponent.clearMainHandItem();
								equippedItemComponent.setMainHandItem(currentEntity, itemHoldingDwarf, messageDispatcher);
								renderEntityWithOrientation(itemHoldingDwarf, orientation, originalPosition, entityRenderer);
								equippedItemComponent.clearMainHandItem();
								if (currentEntity.getComponent(AnimationComponent.class) != null) {
									AnimationComponent dwarfAnimationComponent = itemHoldingDwarf.getOrCreateComponent(AnimationComponent.class);
									dwarfAnimationComponent.setCurrentAnimation(currentEntity.getComponent(AnimationComponent.class).getCurrentAnimation());

								}
							} else {
								renderEntityWithOrientation(currentEntity, orientation, originalPosition, entityRenderer);
							}

						}
					}

					//TODO: sort duplication
					//TODO: consider ui toggle for layout lines
					for (EntityAssetOrientation orientation : EntityAssetOrientation.values()) {
						if (baseAsset.getSpriteDescriptors().containsKey(orientation) && hasSpriteForRenderMode(baseAsset, currentRenderMode, orientation)) {
							renderEntityWithOrientation(itemHoldingDwarf, orientation, originalPosition, entity -> {
								Vector2 worldPosition = entity.getLocationComponent().getWorldPosition();
								shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
								shapeRenderer.setColor(ENTITY_OUTLINE_COLOR);

								shapeRenderer.line(worldPosition.x - 0.5f, worldPosition.y - 0.5f, worldPosition.x + 0.5f, worldPosition.y + 0.5f);
								shapeRenderer.line(worldPosition.x - 0.5f, worldPosition.y + 0.5f, worldPosition.x + 0.5f, worldPosition.y - 0.5f);
								shapeRenderer.end();

								if (currentEntity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes furnitureAttributes) {
									FurnitureLayout currentLayout = furnitureAttributes.getCurrentLayout();
									GridPoint2 tile = VectorUtils.toGridPoint(worldPosition);

									shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
									shapeRenderer.setColor(ENTITY_OUTLINE_COLOR.cpy());
									for (GridPoint2 extraTile : currentLayout.getExtraTiles()) {
										GridPoint2 absoluteTilePoint = tile.cpy().add(extraTile);
										shapeRenderer.rect(absoluteTilePoint.x, absoluteTilePoint.y, 1, 1);
									}

									shapeRenderer.end();

									for (FurnitureLayout.Workspace workspace : currentLayout.getWorkspaces()) {
										GridPoint2 workspaceTile = tile.cpy().add(workspace.getLocation());
										GridPoint2 accessedTile = tile.cpy().add(workspace.getAccessedFrom());
										shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

										shapeRenderer.setColor(WORKSPACE_TILE_COLOR);
										shapeRenderer.rect(workspaceTile.x, workspaceTile.y, 1, 1);

										shapeRenderer.setColor(WORKSPACE_OFFSET_TILE_COLOR);
//										shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
										shapeRenderer.rect(accessedTile.x, accessedTile.y, 1, 1);

										//todo: rocky learn geometry again
//										Vector2 direction = new Vector2(accessedTile.y - workspaceTile.y, accessedTile.x - workspaceTile.x);
//										shapeRenderer.triangle(
//												accessedTile.x, accessedTile.y,
//												workspaceTile.x + (direction.x / 2), workspaceTile.y + (direction.y / 2),
//												accessedTile.x + direction.x, accessedTile.y  + direction.y
//										);
										shapeRenderer.end();

										entity.getLocationComponent().setWorldPosition(VectorUtils.toVector(accessedTile), false, false);
										entity.getLocationComponent().setWorldPosition(VectorUtils.toVector(workspaceTile), true, false);
										entity.getLocationComponent().setWorldPosition(VectorUtils.toVector(accessedTile), false, false);

										spriteBatch.begin();
										spriteBatch.setProjectionMatrix(camera.combined);
										this.entityRenderer.render(entity, spriteBatch, currentRenderMode, null, null, null);
										spriteBatch.end();

										// Reset position
										entity.getLocationComponent().setWorldPosition(originalPosition, false, false);

									}
								}
							});

						}
					}

				}
			}
			ui.render();
		} catch (Throwable e) {
			CrashHandler.logCrash(e);
			throw e;
		}
	}

	private static boolean hasSpriteForRenderMode(EntityAsset baseAsset, RenderMode currentRenderMode, EntityAssetOrientation orientation) {
		if (baseAsset.getSpriteDescriptors().get(orientation).getIsAnimated()) {
			return baseAsset.getSpriteDescriptors().get(orientation).getAnimatedSprites(currentRenderMode) != null;
		} else {
			return baseAsset.getSpriteDescriptors().get(orientation).getSprite(currentRenderMode) != null;
		}
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

	private void renderEntityWithOrientation(Entity entity, EntityAssetOrientation orientation, Vector2 originalPosition, Consumer<Entity> renderBehavior) {
		int spritePadding = editorStateProvider.getState().getSpritePadding();
		float offsetX = orientation.asOriginalVector.x * spritePadding;
		float offsetY = Math.max(orientation.asOriginalVector.y, 0) * spritePadding;

		// Set orientation
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(orientation.asOriginalVector), true, false);
		// Set position
		entity.getLocationComponent().setWorldPosition(originalPosition.cpy().add(offsetX, offsetY), false, false);

		renderBehavior.accept(entity);

		// Reset position
		entity.getLocationComponent().setWorldPosition(originalPosition, false, false);
	}

	@Override
	public void resize (int width, int height) {
		if (ui != null) {
			ui.onResize(width, height);
		}

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
		shapeRenderer.dispose();
		VisUI.dispose();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ENTITY_CREATED: {
				//do nothing
				return true;
			}
			case MessageType.IDENTIFY_PARTICLE_REQUEST_TARGET: {
				//do nothing
				return true;
			}
			case MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME: {
				//do nothing
				return true;
			}
			case MessageType.ENTITY_ASSET_UPDATE_REQUIRED: {
				entityAssetUpdater.updateEntityAssets((Entity) msg.extraInfo);
				return true;
			}
			case MessageType.EDITOR_RELOAD: {
				AssetsPackager.main(editorStateProvider.getState().getModDir());
				init(false);
				return true;
			}
		}
		return true;
	}

}