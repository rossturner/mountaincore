package technology.rocketjump.mountaincore.ui.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.mapping.minimap.MinimapContainer;
import technology.rocketjump.mountaincore.mapping.minimap.MinimapManager;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;

@Singleton
public class MinimapGuiView implements GuiView, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final MinimapManager minimapManager;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final MinimapContainer minimapContainer;
	private final Texture minimapSelectionTexture;
	private final WidgetGroup minimapGroup;
	private Table table;
	private final Button resizeButton;
	private GameContext gameContext;
	private Vector2 initialDragStageCoords = new Vector2();
	private Vector2 initialContainerSize = new Vector2();

	private static final float MIN_CONTAINER_WIDTH = 300f;
	private static final float MAX_CONTAINER_HEIGHT = 1600f;

	@Inject
	public MinimapGuiView(GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						  MinimapManager minimapManager, PrimaryCameraWrapper primaryCameraWrapper, SoundAssetDictionary soundAssetDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.minimapManager = minimapManager;
		this.primaryCameraWrapper = primaryCameraWrapper;

		table = new Table();

		Skin mainGameSkin = guiSkinRepository.getMainGameSkin();
		Drawable resizeButtonDrawable = mainGameSkin.getDrawable("btn_map_resize");
		resizeButton = new Button(resizeButtonDrawable);
		resizeButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		resizeButton.addListener(new ChangeCursorOnHover(resizeButton, GameCursor.RESIZE, messageDispatcher));

		minimapSelectionTexture = new Texture("assets/ui/minimapSelection.png");
		TextureRegionDrawable selectionDrawable = new TextureRegionDrawable(new TextureRegion(minimapSelectionTexture));
		minimapContainer = new MinimapContainer(selectionDrawable, messageDispatcher, mainGameSkin);

		minimapGroup = new WidgetGroup();

		minimapGroup.addActor(minimapContainer);
		minimapGroup.addActor(resizeButton);

		resizeButton.addListener(new InputListener() {

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				messageDispatcher.dispatchMessage(MessageType.SET_SPECIAL_CURSOR, GameCursor.RESIZE);
				initialDragStageCoords = table.getStage().screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
				initialContainerSize = new Vector2(minimapContainer.getContainerWidth(), minimapContainer.getContainerHeight());
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				messageDispatcher.dispatchMessage(MessageType.SET_SPECIAL_CURSOR, null);
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				Vector2 initialLocalCoords = minimapContainer.stageToLocalCoordinates(initialDragStageCoords.cpy());
				Vector2 newLocalCoords = minimapContainer.stageToLocalCoordinates(minimapContainer.getStage().screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY())));
				Vector2 diff = initialLocalCoords.sub(newLocalCoords);

				float aspectRatio = ((float) gameContext.getAreaMap().getWidth() + (2 * MinimapContainer.MINIMAP_FRAME_BORDER_SIZE))
						/ ((float) gameContext.getAreaMap().getHeight() + (2 * MinimapContainer.MINIMAP_FRAME_BORDER_SIZE));

				float newContainerWidth = initialContainerSize.x + diff.x;
				float newContainerHeight = initialContainerSize.y + (diff.x / aspectRatio);

				if (newContainerWidth < MIN_CONTAINER_WIDTH) {
					newContainerWidth = MIN_CONTAINER_WIDTH;
					newContainerHeight = MIN_CONTAINER_WIDTH / aspectRatio;
				} else if (newContainerHeight > MAX_CONTAINER_HEIGHT) {
					newContainerWidth = MAX_CONTAINER_HEIGHT * aspectRatio;
					newContainerHeight = MAX_CONTAINER_HEIGHT;
				}

				minimapContainer.setContainerWidth(newContainerWidth, false);
				minimapContainer.setContainerHeight(newContainerHeight, false);
				resetTable();
			}
		});
	}

	@Override
	public void populate(Table containerTable) {
		resetTable();
		containerTable.add(table).right();
	}

	@Override
	public void update() {
		if (minimapManager.getCurrentTexture() != null) {
			minimapContainer.updateTexture(minimapManager.getCurrentTexture());
		}

		OrthographicCamera camera = primaryCameraWrapper.getCamera();

		minimapContainer.setMapSize(gameContext.getAreaMap().getWidth(), gameContext.getAreaMap().getHeight());
		minimapContainer.setCameraPosition(camera.position);
		minimapContainer.setViewportSize(camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
	}

	@Override
	public GuiViewName getName() {
		return GuiViewName.MINIMAP;
	}

	@Override
	public GuiViewName getParentViewName() {
		return null;
	}

	private void resetTable() {
		table.clearChildren();

		minimapContainer.setSize(minimapContainer.getContainerWidth(), minimapContainer.getContainerHeight());

		table.add(minimapGroup).right()
				.size(minimapContainer.getContainerWidth(), minimapContainer.getContainerHeight())
				.padRight(8).padBottom(8);

		resizeButton.setPosition(minimapContainer.getX(), minimapContainer.getY() + minimapContainer.getContainerHeight() - resizeButton.getHeight());
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		if (gameContext != null) {
			this.gameContext = gameContext;
			if (gameContext.getAreaMap() != null) {
				minimapContainer.setContainerWidth(gameContext.getAreaMap().getWidth(), true);
				minimapContainer.setContainerHeight(gameContext.getAreaMap().getHeight(), true);
				while (minimapContainer.getContainerWidth() > MAX_CONTAINER_HEIGHT) {
					minimapContainer.setContainerWidth(minimapContainer.getContainerWidth() / 2f, false);
					minimapContainer.setContainerHeight(minimapContainer.getContainerHeight() / 2f, false);
				}
				resetTable();
			}
		}
	}

	@Override
	public void clearContextRelatedState() {

	}

}
